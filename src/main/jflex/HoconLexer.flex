/*******************************************************************************
 * Copyright (c) 2025 Guillaume Nodet
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which accompanies this
 * distribution and is available at:
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.maveniverse.maven.mason.hocon;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

%%

%public
%class HoconLexer
%implements AutoCloseable
%unicode
%line
%column
%type HoconToken

%{
  private StringBuilder string = new StringBuilder();
  private int startLine;
  private int startColumn;
  private boolean isOptionalSubstitution = false;
  private boolean inPathContext = true;  // Default to path context initially

  // Using ArrayDeque instead of Stack: true for array context, false for object context
  private java.util.Deque<Boolean> contextStack = new java.util.ArrayDeque<>();

  private HoconToken lastToken = null;
  private String lastTokenValue = null;

  // Track structure depth
  private int objectDepth = 0;
  private int arrayDepth = 0;

  public HoconLexer(String input) {
    this(new StringReader(input));
  }

  private void checkUnclosedStructures() {
    if (objectDepth > 0) {
      throw new HoconParseException("Unclosed object starting at line " + startLine + ", column " + startColumn);
    }
    if (arrayDepth > 0) {
      throw new HoconParseException("Unclosed array starting at line " + startLine + ", column " + startColumn);
    }
  }

  private boolean lastTokenWasUnquotedText() {
    // Don't validate unquoted text when in array context
    if (isInArray()) {
      return false;
    }
    // Also don't validate when we're dealing with a numeric value
    if (lastToken != null && lastTokenValue != null) {
      try {
        Integer.parseInt(lastTokenValue);
        return false; // It's a number, so don't treat it as an unquoted text for validation
      } catch (NumberFormatException e) {
        // Not a number, continue with normal handling
      }
    }
    boolean result = lastToken != null &&
           (lastToken.type() == HoconToken.TokenType.UNQUOTED_TEXT ||
            lastToken.type() == HoconToken.TokenType.PATH_TEXT);
    return result;
  }

  private HoconToken token(HoconToken.TokenType type) {
    lastToken = new HoconToken(type, yytext(), yyline + 1, yycolumn + 1);
    lastTokenValue = yytext();
    return lastToken;
  }

  private HoconToken token(HoconToken.TokenType type, String value) {
    lastToken = new HoconToken(type, value, yyline + 1, yycolumn + 1);
    lastTokenValue = value;
    return lastToken;
  }

  private void enterPathContext() {
    inPathContext = true;
  }

  private void enterValueContext() {
    inPathContext = false;
  }

  private void enterArrayContext() {
    contextStack.push(true);  // true for array
    enterValueContext();
  }

  private void exitArrayContext() {
    contextStack.pop();  // Remove current array context
    // Restore previous context
    if (!contextStack.isEmpty()) {
      if (contextStack.peek()) {
        enterValueContext();  // We're still in an outer array
      } else {
        enterPathContext();   // We're back in an object
      }
    } else {
      enterPathContext();     // Back to root level
    }
  }

  private void enterObjectContext() {
    contextStack.push(false);  // false for object
    enterPathContext();
  }

  private void exitObjectContext() {
    contextStack.pop();  // Remove current object context
    // Restore previous context
    if (!contextStack.isEmpty()) {
      if (contextStack.peek()) {
        enterValueContext();  // We're back in an array
      } else {
        enterPathContext();   // We're still in an outer object
      }
    } else {
      enterPathContext();     // Back to root level
    }
  }

  private void handleNewline() {
    // Enter path context if we're at root level or in an object context
    if (contextStack.isEmpty() || !contextStack.peek()) {
      enterPathContext();
    }
  }

  private boolean isInArray() {
    boolean inArray = !contextStack.isEmpty() && contextStack.peek();
    return inArray;
  }

  @Override
  public void close() throws IOException {
    yyclose();
  }

  // Count occurrences of a pattern in a string
  private int countOccurrences(String text, String pattern) {
    int count = 0;
    int index = 0;
    while ((index = text.indexOf(pattern, index)) != -1) {
      count++;
      index += pattern.length();
    }
    return count;
  }

  private String processEscapeSequence(String escape) {
    switch (escape.charAt(1)) {
      case 'b': return "\b";
      case 'f': return "\f";
      case 'n': return "\n";
      case 'r': return "\r";
      case 't': return "\t";
      case '"': return "\"";
      case '\\': return "\\";
      case '/': return "/";
      default: return escape;
    }
  }

  private String processUnicodeEscape(String escape) {
    String hex = escape.substring(2);
    int code = Integer.parseInt(hex, 16);
    return String.valueOf((char) code);
  }

  public List<HoconToken> tokenize() throws java.io.IOException {
      List<HoconToken> tokens = new ArrayList<>();
      HoconToken token;

      while ((token = yylex()) != null) {
          tokens.add(token);
          if (token.type() == HoconToken.TokenType.EOF) {
              break;
          }
      }

      return tokens;
  }
%}

/* Basic macros */
LineTerminator = \r|\n|\r\n
WhiteSpace = [ \t\f]+
Comment = "//"[^\r\n]* | "#"[^\r\n]* | "/*" ~"*/"

/* String components */
StringCharacter = [^\n\r\"\\]

/* Keywords */
Include = [iI][nN][cC][lL][uU][dD][eE]

/* Character classes for unquoted strings */
AlphaNum = [A-Za-z0-9]
Number = [0-9]+
PathChar = ({AlphaNum} | [-_/])+          // Path elements before/after dots
ValueChar = ({AlphaNum} | [-_./])+        // Characters allowed in values

/* States and rules */
%state STRING
%state MULTILINE_STRING
%state SUBSTITUTION
%state UNQUOTED
%state OPTIONAL_SUBSTITUTION
%state PATH
%state QUOTED_PATH
%state INCLUDE

%%
/* Keywords and punctuation */
<YYINITIAL> {
  {WhiteSpace}         { return token(HoconToken.TokenType.WHITESPACE); }
  {LineTerminator}     {
                        handleNewline();
                        return token(HoconToken.TokenType.NEWLINE);
                      }
  {Include}            {
                         yybegin(INCLUDE);
                         return token(HoconToken.TokenType.INCLUDE);
                       }
  /* First handle the valid separators */
  ":"                {
                       enterValueContext();
                       return token(HoconToken.TokenType.COLON);
                     }
  "="                {
                       enterValueContext();
                       return token(HoconToken.TokenType.EQUALS);
                     }

  /* Handle structural characters that should error when following unquoted text */
  "{"                {
                       if (lastTokenWasUnquotedText() && !isInArray()) {
                         throw new HoconParseException(
                           "Key '" + lastTokenValue + "' may not be followed by token: '{' (if you intended '{' to be part of a key or string value, try enclosing the key or value in double quotes)"
                         );
                       }
                       objectDepth++;
                       startLine = yyline + 1;
                       startColumn = yycolumn + 1;
                       enterObjectContext();
                       return token(HoconToken.TokenType.LEFT_BRACE);
                     }
  "}"                {
                       if (lastTokenWasUnquotedText() && !isInArray()) {
                         throw new HoconParseException(
                           "Key '" + lastTokenValue + "' may not be followed by token: '}' (if you intended '}' to be part of a key or string value, try enclosing the key or value in double quotes)"
                         );
                       }
                       if (objectDepth <= 0) {
                         throw new HoconParseException(
                           "Unexpected closing brace '}' at line " + (yyline + 1) + ", column " + (yycolumn + 1)
                         );
                       }
                       objectDepth--;
                       exitObjectContext();
                       return token(HoconToken.TokenType.RIGHT_BRACE);
                     }
  "["                {
                       if (lastTokenWasUnquotedText() && !isInArray()) {
                         throw new HoconParseException(
                           "Key '" + lastTokenValue + "' may not be followed by token: '[' (if you intended '[' to be part of a key or string value, try enclosing the key or value in double quotes)"
                         );
                       }
                       arrayDepth++;
                       startLine = yyline + 1;
                       startColumn = yycolumn + 1;
                       enterArrayContext();
                       return token(HoconToken.TokenType.LEFT_BRACKET);
                     }
  "]"                {
                       if (lastTokenWasUnquotedText() && !isInArray()) {
                         throw new HoconParseException(
                           "Key '" + lastTokenValue + "' may not be followed by token: ']' (if you intended ']' to be part of a key or string value, try enclosing the key or value in double quotes)"
                         );
                       }
                       if (arrayDepth <= 0) {
                         throw new HoconParseException(
                           "Unexpected closing bracket ']' at line " + (yyline + 1) + ", column " + (yycolumn + 1)
                         );
                       }
                       arrayDepth--;
                       exitArrayContext();
                       return token(HoconToken.TokenType.RIGHT_BRACKET);
                     }
  ","                {
                       // Special handling for arrays with comma-separated values like [1, 2]
                       if (isInArray()) {
                         // In array context, just return the comma token
                         enterValueContext();
                         return token(HoconToken.TokenType.COMMA);
                       }
                       
                       // For non-array contexts, throw exception if comma follows unquoted text (key)
                       if (lastTokenWasUnquotedText()) {
                         throw new HoconParseException(
                           "Key '" + lastTokenValue + "' may not be followed by token: ',' (if you intended ',' to be part of a key or string value, try enclosing the key or value in double quotes)"
                         );
                       }
                       
                       // In object context, comma after values
                       enterPathContext();
                       return token(HoconToken.TokenType.COMMA);
                     }
  {LineTerminator}     { return token(HoconToken.TokenType.NEWLINE); }
  {WhiteSpace}        { return token(HoconToken.TokenType.WHITESPACE); }

  /* Handle path or unquoted text based on context */
  {Number}           {
                       if (inPathContext && !isInArray()) {
                         return token(HoconToken.TokenType.PATH_TEXT);
                       } else {
                         if (isInArray()) {
                           // In array context, don't switch to unquoted state to avoid triggering the key validation
                           return token(HoconToken.TokenType.UNQUOTED_TEXT);
                         } else {
                           yybegin(UNQUOTED);
                           return token(HoconToken.TokenType.UNQUOTED_TEXT);
                         }
                       }
                     }
  {PathChar}         {
                       if (inPathContext && !isInArray()) {
                         return token(HoconToken.TokenType.PATH_TEXT);
                       } else {
                         if (isInArray()) {
                           // In array context, don't switch to unquoted state to avoid triggering the key validation
                           return token(HoconToken.TokenType.UNQUOTED_TEXT);
                         } else {
                           yybegin(UNQUOTED);
                           return token(HoconToken.TokenType.UNQUOTED_TEXT);
                         }
                       }
                     }
  {ValueChar}        {
                       if (inPathContext == false || isInArray()) {
                         if (isInArray()) {
                           // In array context, don't switch to unquoted state to avoid triggering the key validation
                           return token(HoconToken.TokenType.UNQUOTED_TEXT);
                         } else {
                           yybegin(UNQUOTED);
                           return token(HoconToken.TokenType.UNQUOTED_TEXT);
                         }
                       } else {
                         return token(HoconToken.TokenType.PATH_TEXT);
                       }
                     }
  /* Reset path context only after specific tokens */
  "${"                 { string.setLength(0);
                         startLine = yyline + 1;
                         startColumn = yycolumn + 1;
                         yybegin(SUBSTITUTION); }
  "${?}"               { throw new HoconParseException(
                        "Empty optional substitution at line " + (yyline + 1) + ", column " + (yycolumn + 1)
                      ); }
  "${?"                { string.setLength(0);
                         startLine = yyline + 1;
                         startColumn = yycolumn + 1;
                         yybegin(OPTIONAL_SUBSTITUTION); }
  "\""                  { string.setLength(0);
                         startLine = yyline + 1;
                         startColumn = yycolumn + 1;
                         if (inPathContext) {
                           yybegin(QUOTED_PATH);
                         } else {
                           yybegin(STRING);
                         }
                       }
  "\"\"\""             { string.setLength(0);
                         startLine = yyline + 1;
                         startColumn = yycolumn + 1;
                         yybegin(MULTILINE_STRING); }
  {Comment}            {
    String text = yytext();
    if (text.startsWith("/*")) {
      // Don't trim multiline comments to preserve their structure
      // If the comment contains a newline, we should reset to path context
      // This ensures that keys after comments are properly recognized
      if (text.contains("\n")) {
        enterPathContext(); // Reset to path context if the comment spans multiple lines
      }
      return token(HoconToken.TokenType.BLOCK_COMMENT, text.substring(2, text.length()-2));
    } else {
      return token(HoconToken.TokenType.COMMENT, text.substring(text.startsWith("//") ? 2 : 1).trim());
    }
  }
}

<PATH> {
  "."                   { return token(HoconToken.TokenType.DOT); }
  {PathChar}           { return token(HoconToken.TokenType.PATH_TEXT); }
  "${"                { string.setLength(0);
                        startLine = yyline + 1;
                        startColumn = yycolumn + 1;
                        yybegin(SUBSTITUTION); }
  "${?}"              { throw new HoconParseException(
                        "Empty substitution at line " + (yyline + 1) + ", column " + (yycolumn + 1)
                      ); }
  "$"                 { throw new HoconParseException(
                        "Invalid character '$' at line " + (yyline + 1) + ", column " + (yycolumn + 1)
                      ); }
  [^]                 { yypushback(1); yybegin(YYINITIAL); }
}

<UNQUOTED> {
  {ValueChar}           { return token(HoconToken.TokenType.UNQUOTED_TEXT); }
  "${"                { string.setLength(0);
                        startLine = yyline + 1;
                        startColumn = yycolumn + 1;
                        yybegin(SUBSTITUTION); }
  "${?}"              { throw new HoconParseException(
                        "Empty substitution at line " + (yyline + 1) + ", column " + (yycolumn + 1)
                      ); }
  "$"                 { throw new HoconParseException(
                        "Invalid character '$' at line " + (yyline + 1) + ", column " + (yycolumn + 1)
                      ); }
  [^]                 { yypushback(1); yybegin(YYINITIAL); }
}

<QUOTED_PATH> {
  \"                             { yybegin(YYINITIAL); 
                                  return token(HoconToken.TokenType.QUOTED_PATH, "\"" + string.toString() + "\""); }
  {StringCharacter}+             { string.append(yytext()); }
  \\\"                          { string.append("\\\""); }  // Preserve escaped quotes
  \\.                           { string.append(yytext()); }
  \n                            { throw new HoconParseException("Unclosed quoted path at line " + startLine + ", column " + startColumn); }
  <<EOF>>                       { throw new HoconParseException("Unclosed quoted path at line " + startLine + ", column " + startColumn); }
}

<STRING> {
  \"                             { yybegin(YYINITIAL); 
                                  return token(HoconToken.TokenType.STRING, "\"" + string.toString() + "\""); }
  {StringCharacter}+             { string.append(yytext()); }
  \\\"                          { string.append("\\\""); }  // Preserve escaped quotes
  \\.                           { string.append(yytext()); }
  \n                            { throw new HoconParseException("Unclosed string literal at line " + startLine + ", column " + startColumn); }
  <<EOF>>                       { throw new HoconParseException("Unclosed string literal at line " + startLine + ", column " + startColumn); }
}

<MULTILINE_STRING> {
  \"\"\"              { yybegin(YYINITIAL);
                        return token(HoconToken.TokenType.MULTILINE_STRING, string.toString()); }
  [^\"\\]+            { string.append(yytext()); }
  \"                  { string.append("\""); }
  "\\"                { string.append("\\"); }  // Add literal backslash with no escaping
  [\n\r]              { string.append(yytext()); }
  <<EOF>>            { throw new HoconParseException(
                        "Unclosed multiline string starting at line " + startLine + ", column " + startColumn); }
}

<SUBSTITUTION> {
  "${"                { string.append(yytext()); }
  "}"                  {
                         if (string.length() == 0) {
                           throw new HoconParseException(
                             "Empty substitution at line " + startLine + ", column " + startColumn
                           );
                         }

                         // Check if the braces are balanced
                         String current = string.toString();
                         int openBraces = countOccurrences(current, "${");
                         int closeBraces = countOccurrences(current, "}");

                         if (openBraces > closeBraces) {
                           // If we still have open braces, we need to keep parsing
                           string.append(yytext());
                         } else {
                           // If braces are balanced, we're done
                           yybegin(YYINITIAL);
                           return token(HoconToken.TokenType.SUBSTITUTION, string.toString());
                         }
                       }
  [^}$]+              { string.append(yytext()); }
  "$"                 { string.append(yytext()); }
  <<EOF>>             { throw new HoconParseException(
                        "Unclosed substitution starting at line " + startLine + ", column " + startColumn); }
}

<OPTIONAL_SUBSTITUTION> {
  "${"                { string.append(yytext()); }
  "}"                  {
                         if (string.length() == 0) {
                           throw new HoconParseException(
                             "Empty optional substitution at line " + startLine + ", column " + startColumn
                           );
                         }

                         // Check if the braces are balanced
                         String current = string.toString();
                         int openBraces = countOccurrences(current, "${");
                         int closeBraces = countOccurrences(current, "}");

                         if (openBraces > closeBraces) {
                           // If we still have open braces, we need to keep parsing
                           string.append(yytext());
                         } else {
                           // If braces are balanced, we're done
                           yybegin(YYINITIAL);
                           return token(HoconToken.TokenType.OPTIONAL_SUBSTITUTION, string.toString());
                         }
                       }
  [^}$]+              { string.append(yytext()); }
  "$"                 { string.append(yytext()); }
  <<EOF>>             { throw new HoconParseException(
                        "Unclosed substitution starting at line " + startLine + ", column " + startColumn); }
}

/* After INCLUDE keyword, expect a string */
<INCLUDE> {
  {WhiteSpace}         { return token(HoconToken.TokenType.WHITESPACE); }
  \"                   { string.setLength(0);
                         startLine = yyline + 1;
                         startColumn = yycolumn + 1;
                         yybegin(STRING); }
  [^]                  { throw new HoconParseException(
                         "Expected quoted string after include at line " + (yyline + 1) + ", column " + (yycolumn + 1)
                       ); }
}

/* Error fallback */
[^]                 { 
  throw new HoconParseException(
    "Invalid character '" + yytext() + "' at line " + (yyline + 1) + ", column " + (yycolumn + 1)
  ); 
}

<<EOF>>              { 
                       checkUnclosedStructures();
                       return token(HoconToken.TokenType.EOF); 
                     }
