// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.youngandroid;

import com.google.appinventor.client.Ode;
import static com.google.appinventor.client.Ode.MESSAGES;

import com.google.appinventor.client.editor.simple.SimpleComponentDatabase;
import com.google.appinventor.client.editor.youngandroid.YaProjectEditor;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;

import java.util.Arrays;
import java.util.List;

/**
 */
public final class TextValidators {

  private static final int MAX_FILENAME_SIZE = 100;
  private static final int MIN_FILENAME_SIZE = 1;

  protected static final List<String> YAIL_NAMES = Arrays.asList("CsvUtil", "Double", "Float",
          "Integer", "JavaCollection", "JavaIterator", "KawaEnvironment", "Long", "Short",
          "SimpleForm", "String", "Pattern", "YailList", "YailNumberToString", "YailRuntimeError");

  // This class should never be instantiated.
  private TextValidators() {}

  /**
   * Determines whether the given project name is valid, displaying an alert
   * if it is not.  In order to be valid, the project name must satisfy
   * {@link #isValidIdentifier(String)} and not be a duplicate of an existing
   * project name for the same user.
   *
   * @param projectName the project name to validate
   * @return {@code true} if the project name is valid, {@code false} otherwise
   */
  public static boolean checkNewProjectName(String projectName) {

    // Check the format of the project name
    if (!isValidIdentifier(projectName)) {
      Window.alert(MESSAGES.malformedProjectNameError());
      return false;
    }

    // Check that project does not already exist
    if (Ode.getInstance().getProjectManager().getProject(projectName) != null) {
      Window.alert(MESSAGES.duplicateProjectNameError(projectName));
      return false;
    }

    return true;
  }

  public static boolean checkNewComponentName(String componentName) {

    // Check that it meets the formatting requirements.
    if (!TextValidators.isValidComponentIdentifier(componentName)) {
      Window.alert(MESSAGES.malformedComponentNameError());
      return false;
    }

    long projectId = Ode.getInstance().getCurrentYoungAndroidProjectId();
    if ( projectId == 0) { // Check we have a current Project
      return false;
    }

    YaProjectEditor editor = (YaProjectEditor) Ode.getInstance().getEditorManager().getOpenProjectEditor(projectId);

    // Check that it's unique.
    final List<String> names = editor.getComponentInstances();
    if (names.contains(componentName)) {
      Window.alert(MESSAGES.sameAsComponentInstanceNameError());
      return false;
    }

    // Check that it is a variable name used in the Yail code
    if (YAIL_NAMES.contains(componentName)) {
      Window.alert(MESSAGES.badComponentNameError());
      return false;
    }

    //Check that it is not a Component type name
    SimpleComponentDatabase COMPONENT_DATABASE = SimpleComponentDatabase.getInstance(projectId);
    if (COMPONENT_DATABASE.isComponent(componentName)) {
      Window.alert(MESSAGES.duplicateComponentNameError());
      return false;
    }

    return true;
  }

  /**
   * Checks whether the argument is a legal identifier, specifically,
   * a non-empty string starting with a letter and followed by any number of
   * (unaccented English) letters, digits, or underscores.
   *
   * @param text the proposed identifier
   * @return {@code true} if the argument is a legal identifier, {@code false}
   *         otherwise
   */
  public static boolean isValidIdentifier(String text) {
    return text.matches("^[a-zA-Z]\\w*$");
  }

  /**
   * Checks whether the argument is a legal component identifier; please check
   * Blockly.LexicalVariable.checkIdentifier for the regex reference
   *
   * @param text the proposed identifier
   * @return {@code true} if the argument is a legal identifier, {@code false}
   *         otherwise
   */
  public static boolean isValidComponentIdentifier(String text) {
    return text.matches("^[^-0-9!&%^/>=<`'\"#:;,\\\\^\\*\\+\\.\\(\\)\\|\\{\\}\\[\\]\\ ]" +
        "[^-!&%^/>=<'\"#:;,\\\\^\\*\\+\\.\\(\\)\\|\\{\\}\\[\\]\\ ]*$");
  }

  /**
   * Checks whether the argument is a legal filename, meaning
   * it is unchanged by URL encoding and it meets the aapt
   * requirements as follows:
   * - all characters must be 7-bit printable ASCII
   * - none of { '/' '\\' ':' }
   * @param filename The filename (not path) of uploaded file
   * @return {@code true} if the argument is a legal filename, {@code false}
   *         otherwise
   */
  public static boolean isValidCharFilename(String filename){
    return !filename.contains("'") && filename.equals(URL.encodePathSegment(filename));
    //returns the logic of the file name doesn't contain any "'" (true if there is not "'" in Filename)
    //and whether if filename.equals(URL.encodePathSegment)  )
    //encodePathSegment takes away all invalid characters in the filename - so if filename equals 
    //encodePathSegment, that means that there were no invalid characters, so it will evaulate true
    //evaluates true if filename doesnt have single quotes and if filename doesnt have any invalid characters
  }
  
  /**
   * Checks whether the argument is a filename which meets the length
   * requirement imposed by aapt, which is:
   * - the filename length must be less than kMaxAssetFileName bytes long
   *   (and can't be empty)
   * where kMaxAssetFileName is defined to be 100.
   * (A legal name, therefore, has length <= kMaxAssetFileNames)
   * @param filename The filename (not path) of uploaded file
   * @return {@code true} if the length of the argument is legal, {@code false}
   *         otherwise
   */
  public static boolean isValidLengthFilename(String filename){
    return !(filename.length() > MAX_FILENAME_SIZE || filename.length() < MIN_FILENAME_SIZE);
    //returns the logic of the opposite of (if filename.length is larger than MAX_FILENAME_SIZE)
    //will return False if filename is larger than MAX_FILENAME_SIZE (100) or if its smaller than MIN_FILENAME_SIZE (1)
    //will return True if filename is smaller than MAX_FILENAME_SIZE (100) or if its larger than MIN_FILENAME_SIZE (1)
  }

  /**
   * Determines human-readable message for specific error.
   * @param filename The filename (not path) of uploaded file
   * @return String representing error message, empty string if no error
   */
  public static String getErrorMessage(String filename){
    String errorMessage = "";
    String noWhitespace = "[\\S]+";
    String firstCharacterLetter = "[A-Za-z].*";
    if(!filename.matches("[A-Za-z][A-Za-z0-9_]*") && filename.length() > 0) {
      if(!filename.matches(noWhitespace)) { //Check to make sure that this project does not contain any whitespace
        errorMessage = MESSAGES.whitespaceProjectNameError();
      } else if (!filename.matches(firstCharacterLetter)) { //Check to make sure that the first character is a letter
        errorMessage = MESSAGES.firstCharProjectNameError();
      } else { //The text contains a character that is not a letter, number, or underscore
        errorMessage = MESSAGES.invalidCharProjectNameError();
      }
    }
    return errorMessage;
  }
}
