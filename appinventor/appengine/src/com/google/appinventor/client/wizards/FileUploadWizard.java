// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.wizards;

import static com.google.appinventor.client.Ode.MESSAGES;

import java.io.File;

import com.google.appinventor.client.ErrorReporter;
import com.google.appinventor.client.Ode;
import com.google.appinventor.client.OdeAsyncCallback;
import com.google.appinventor.client.explorer.project.Project;
import com.google.appinventor.client.utils.Uploader;
import com.google.appinventor.client.youngandroid.TextValidators;
import com.google.appinventor.shared.rpc.ServerLayout;
import com.google.appinventor.shared.rpc.UploadResponse;
import com.google.appinventor.shared.rpc.project.FileNode;
import com.google.appinventor.shared.rpc.project.FolderNode;
import com.google.appinventor.shared.rpc.project.ProjectNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidAssetNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidAssetsFolder;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;


/**
 * Wizard for uploading individual files.
 *
 */
public class FileUploadWizard extends Wizard {
  /**
   * Interface for callback to execute after a file is uploaded.
   */
  public static interface FileUploadedCallback {
    /**
     * Will be invoked after a file is uploaded.
     *
     * @param folderNode the upload destination folder
     * @param fileNode the file just uploaded
     */
    void onFileUploaded(FolderNode folderNode, FileNode fileNode);
  }

  /**
   * Creates a new file upload wizard.
   *
   * @param folderNode the upload destination folder
   */
  public FileUploadWizard(FolderNode folderNode) {
    this(folderNode, null);
  }

  /**
   * Creates a new file upload wizard.
   *
   * @param folderNode the upload destination folder
   * @param fileUploadedCallback callback to be executed after upload
   */
  public FileUploadWizard(final FolderNode folderNode,
      final FileUploadedCallback fileUploadedCallback) {
    super(MESSAGES.fileUploadWizardCaption(), true, false);

    // Initialize UI
    final FileUpload upload = new FileUpload();                  //Makes an instance of FileUpload Widget called upload
    upload.setName(ServerLayout.UPLOAD_FILE_FORM_ELEMENT);       //ServerLayout.UPLOAD_FILE_FORM_ELEMENT = "uploadFile", sets the uploadWidget name to be "uploadFile"
    setStylePrimaryName("ode-DialogBox");                         
    VerticalPanel panel = new VerticalPanel();                   //Makes an instace of the VerticalPanel called panel
    panel.setVerticalAlignment(VerticalPanel.ALIGN_BOTTOM);      //Sets the panel to setVerticalAlignment to Align Middle
    panel.add(upload);                                           //Adds the upload instance to the panel.
    addPage(panel);                                              //Adds the panel to the page.

    // Create finish command (upload a file)
    initFinishCommand(new Command() {
      @Override
      public void execute() {                                    //new command execute 
        String uploadFilename = upload.getFilename();            //gets the Filename of the upload to the variable uploadFilename
        if (!uploadFilename.isEmpty()) {                         //If this uploadFilename of the upload is not empty 
          final String filename = makeValidFilename(uploadFilename);  //Calls the makeValidFilename with the uploadFilename returns a 
          if(!TextValidators.isValidCharFilename(filename)){          //If TextValidators.isValidCharfilename returns False with filename passed, has single quotes or invalid characters,
            createErrorDialog(MESSAGES.malformedFilenameTitle(), MESSAGES.malformedFilename(), //Error will come up with No Files Selected
              Error.NOFILESELECETED, folderNode, fileUploadedCallback);
            return;                                               //return nothing
          } else if (!TextValidators.isValidLengthFilename(filename)){ //If TextValidators.isValidLengthFilename(filename) returns FALSE (has too long or too short of a file name)
            createErrorDialog(MESSAGES.filenameBadSizeTitle(), MESSAGES.filenameBadSize(),
              Error.FILENAMEBADSIZE, folderNode, fileUploadedCallback); //Error will come up with FileNameBadSize
            return;                                               //returns nothing
          }
          int nameLength = uploadFilename.length();               //takes the length of the uploadFilename and sets its to nameLength
          String fileEnd = uploadFilename.substring(nameLength-4, nameLength); //Makes the String fileEnd equal to the file property suffix (.aia, .apk, etc)

          if (".aia".equals(fileEnd.toLowerCase())) {                                 //if the fileEnd equals to ".AIA", then another Error will come up with
            createErrorDialog(MESSAGES.aiaMediaAssetTitle(), MESSAGES.aiaMediaAsset(), //AIAMEDIAASSET
              Error.AIAMEDIAASSET, folderNode, fileUploadedCallback);
            return; //returns nothing
          }
          String fn = conflictingExistingFile(folderNode, filename);  //Makes a string called fn and sets it equal to conflictExistingFile()
          if (fn != null && !confirmOverwrite(folderNode, fn, filename)) { //if the fn is not null and user doesn't want to overwrite the file
            return; //return nothing
          } else {
            String fileId = folderNode.getFileId() + "/" + filename; //otherwise, set fileID to folderNODE.getFILEID()
            // We delete all the conflicting files.
            for (ProjectNode child : folderNode.getChildren()) { //for each projectNode child in folerNode.getChildren()
              if (fileId.equalsIgnoreCase(child.getFileId()) && !fileId.equals(child.getFileId())) { //if the fileID = child.GetFileID and the fileID doesn't equl child.getfileID
                final ProjectNode node = child;                                                 //(if the fileID is equal to the child.getfileId without case and it doesnt equal child.getFileID considerinf case) (Different cases)
                String filesToClose [] = { node.getFileId()};                                 //set the node equal to the child , creates an array filesToClose with that child node's file ID in it 
                Ode ode = Ode.getInstance();                                            
                ode.getEditorManager().closeFileEditors(node.getProjectId(), filesToClose);
                ode.getProjectService().deleteFile(ode.getSessionId(),
                    node.getProjectId(), node.getFileId(),
                    new OdeAsyncCallback<Long>(
                        // message on failure
                        MESSAGES.deleteFileError()) {
                      @Override
                      public void onSuccess(Long date) {
                        Ode.getInstance().getProjectManager().getProject(node).deleteNode(node);
                        Ode.getInstance().updateModificationDate(node.getProjectId(), date);

                      }
                    });
              }
            }
          }
          ErrorReporter.reportInfo(MESSAGES.fileUploadingMessage(filename)); //Uploading the file to the server   @DefaultMessage("Uploading {0} to the App Inventor server")

          // Use the folderNode's project id and file id in the upload URL so that the file is
          // uploaded into that project and that folder in our back-end storage.
          String uploadUrl = GWT.getModuleBaseURL() + ServerLayout.UPLOAD_SERVLET + "/" + //uploadURL = urlprefix + upload + / + file
              ServerLayout.UPLOAD_FILE + "/" + folderNode.getProjectId() + "/" + //+ / folderNode.getProjectId() + / + folderNode.getFileId() + "/" + filename
              folderNode.getFileId() + "/" + filename;
              ErrorReporter.reportInfo(uploadUrl);
          Uploader.getInstance().upload(upload, uploadUrl,   //calls upload using upload widget, uploadURL created the line above
              new OdeAsyncCallback<UploadResponse>(MESSAGES.fileUploadError()) { //returns with a error to wait later if it fails on callback
            @Override
            public void onSuccess(UploadResponse uploadResponse) {  //on success for uploader
              switch (uploadResponse.getStatus()) {    //gets the status of uploadResponse
              case SUCCESS: 
                ErrorReporter.hide(); //hides the erroReported
                onUploadSuccess(folderNode, filename, uploadResponse.getModificationDate(),
                    fileUploadedCallback); //updates modification date
                break;
              case FILE_TOO_LARGE:
                // The user can resolve the problem by
                // uploading a smaller file.
                ErrorReporter.reportInfo(MESSAGES.fileTooLargeError()); //Error for file too Larger
                break;
              default:
                ErrorReporter.reportError(MESSAGES.fileUploadError()); //Error for file didnt upload
                break;
              }
            }
          });
        } else {
          createErrorDialog(MESSAGES.noFileSelectedTitle(), MESSAGES.noFileSelected(),
              Error.NOFILESELECETED, folderNode, fileUploadedCallback); //no files was selected
        }
      }
    });
  }

  @Override
  public void show() {
    super.show();
    int width = 320;
    int height = 40;
    this.center();

    setPixelSize(width, height);
    super.setPagePanelHeight(40);
  }

  private String makeValidFilename(String uploadFilename) {
    // Strip leading path off filename.
    // We need to support both Unix ('/') and Windows ('\\') separators.
    String filename = uploadFilename.substring(
        Math.max(uploadFilename.lastIndexOf('/'), uploadFilename.lastIndexOf('\\')) + 1);
    // We need to strip out whitespace from the filename.
    filename = filename.replaceAll("\\s", "");
    return filename;
  }

  private String conflictingExistingFile(FolderNode folderNode, String filename) { 
    String fileId = folderNode.getFileId() + "/" + filename;
    for (ProjectNode child : folderNode.getChildren()) {
      if (fileId.equalsIgnoreCase(child.getFileId())) {
        // we want to return kitty.png rather than assets/kitty.png
        //return lastPathComponent(child.getFileId()); //see full path
        return child.getFileId();
      }
    }
    return null;
  }

  private String lastPathComponent (String path) {
    String [] pieces = path.split("/");
    return pieces[pieces.length - 1];
  }

  private boolean confirmOverwrite(FolderNode folderNode, String newFile, String existingFile) {
    return Window.confirm(MESSAGES.confirmOverwrite(newFile, existingFile));
  }

  private void onUploadSuccess(final FolderNode folderNode, final String filename,
      long modificationDate, final FileUploadedCallback fileUploadedCallback) {
    //Ode.getInstance().updateModificationDate(folderNode.getProjectId(), modificationDate);
    finishUpload(folderNode, filename, fileUploadedCallback);
  }

  private void finishUpload(FolderNode folderNode, String filename,
      FileUploadedCallback fileUploadedCallback) {
    String uploadedFileId = folderNode.getFileId() + "/" + filename;
    FileNode uploadedFileNode;
    if (folderNode instanceof YoungAndroidAssetsFolder) {
      uploadedFileNode = new YoungAndroidAssetNode(filename, uploadedFileId);

    } else {
      uploadedFileNode = new FileNode(filename, uploadedFileId);
    }
    Window.confirm("uploadedFileNode's getFileId(): " + uploadedFileNode.getFileId());

    Project project = Ode.getInstance().getProjectManager().getProject(folderNode);


    uploadedFileNode = (FileNode) project.addNode(folderNode, uploadedFileNode);
    Window.confirm("uploadedFileNode's getFileId() after addNode " + uploadedFileNode.getFileId());

    Iterable<ProjectNode> childrenNodes = project.getRootNode().getChildren();

    for (ProjectNode node : childrenNodes) {
        Window.confirm("getFullName: " + node.getFullName());
        Window.confirm("getFileId: " + node.getFileId());
    }

    /*if (fileUploadedCallback != null) {
      fileUploadedCallback.onFileUploaded(folderNode, uploadedFileNode);
    }*/
  }

  private void createErrorDialog(String title, String body, Error e,
      final FolderNode folderNode, final FileUploadedCallback fileUploadedCallback) {
    final DialogBox dialogBox = new DialogBox(false,true);
    HTML message;
    dialogBox.setStylePrimaryName("ode-DialogBox");
    dialogBox.setHeight("150px");
    dialogBox.setWidth("350px");
    dialogBox.setGlassEnabled(true);
    dialogBox.setAnimationEnabled(true);
    dialogBox.center();
    VerticalPanel DialogBoxContents = new VerticalPanel();
    FlowPanel holder = new FlowPanel();
    Button ok = new Button ("OK");
    ok.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        dialogBox.hide();
        new FileUploadWizard(folderNode, fileUploadedCallback).show();
      }
    });
    holder.add(ok);
    dialogBox.setText(title);
    message = new HTML(body);

    switch(e) {
      case AIAMEDIAASSET:
        Button info = new Button ("More Info");
        info.addClickListener(new ClickListener() {
          public void onClick(Widget sender) {
            Window.open(MESSAGES.aiaMediaAssetHelp(), "AIA Help", "");
          }
        });
        holder.add(info);
      case NOFILESELECETED:
      case MALFORMEDFILENAME:
      case FILENAMEBADSIZE:
      default:
        break;
    }

    message.setStyleName("DialogBox-message");
    DialogBoxContents.add(message);
    DialogBoxContents.add(holder);
    dialogBox.setWidget(DialogBoxContents);
    dialogBox.show();
  }

}

enum Error {
  AIAMEDIAASSET, NOFILESELECETED, MALFORMEDFILENAME, FILENAMEBADSIZE
}
