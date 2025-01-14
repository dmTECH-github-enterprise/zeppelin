/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zeppelin.interpreter;

import org.apache.commons.io.FileUtils;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.display.AngularObjectRegistryListener;
import org.apache.zeppelin.helium.ApplicationEventListener;
import org.apache.zeppelin.interpreter.remote.RemoteInterpreterProcessListener;
import org.apache.zeppelin.notebook.AuthorizationService;
import org.apache.zeppelin.notebook.GsonNoteParser;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.NoteManager;
import org.apache.zeppelin.notebook.NoteParser;
import org.apache.zeppelin.notebook.Notebook;
import org.apache.zeppelin.notebook.repo.InMemoryNotebookRepo;
import org.apache.zeppelin.notebook.repo.NotebookRepo;
import org.apache.zeppelin.plugin.PluginManager;
import org.apache.zeppelin.storage.ConfigStorage;
import org.apache.zeppelin.user.Credentials;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.mockito.Mockito.mock;

/**
 * This class will load configuration files under
 *   src/test/resources/interpreter
 *   src/test/resources/conf
 *
 * to construct InterpreterSettingManager and InterpreterFactory properly
 *
 */
public abstract class AbstractInterpreterTest {
  protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractInterpreterTest.class);

  protected InterpreterSettingManager interpreterSettingManager;
  protected InterpreterFactory interpreterFactory;
  protected NoteParser noteParser;
  protected Notebook notebook;
  protected File zeppelinHome;
  protected File interpreterDir;
  protected File confDir;
  protected File notebookDir;
  protected ZeppelinConfiguration zConf;
  protected ConfigStorage storage;
  protected PluginManager pluginManager;

  @BeforeEach
  public void setUp() throws Exception {
    // copy the resources files to a temp folder
    zeppelinHome = new File("..");
    LOGGER.info("ZEPPELIN_HOME: " + zeppelinHome.getAbsolutePath());
    interpreterDir = new File(zeppelinHome, "interpreter_" + getClass().getSimpleName());
    confDir = new File(zeppelinHome, "conf_" + getClass().getSimpleName());
    notebookDir = new File(zeppelinHome, "notebook_" + getClass().getSimpleName());
    FileUtils.deleteDirectory(notebookDir);

    // Create test directories
    interpreterDir.mkdirs();
    confDir.mkdirs();
    notebookDir.mkdirs();
    // Clean-up the test directories on exit
    FileUtils.forceDeleteOnExit(interpreterDir);
    FileUtils.forceDeleteOnExit(confDir);
    FileUtils.forceDeleteOnExit(notebookDir);

    FileUtils.copyDirectory(new File("src/test/resources/interpreter"), interpreterDir);
    FileUtils.copyDirectory(new File("src/test/resources/conf"), confDir);

    zConf = ZeppelinConfiguration.load();
    zConf.setProperty(ZeppelinConfiguration.ConfVars.ZEPPELIN_HOME.getVarName(),
        zeppelinHome.getAbsolutePath());
    zConf.setProperty(ZeppelinConfiguration.ConfVars.ZEPPELIN_CONF_DIR.getVarName(),
        confDir.getAbsolutePath());
    zConf.setProperty(ZeppelinConfiguration.ConfVars.ZEPPELIN_INTERPRETER_DIR.getVarName(),
        interpreterDir.getAbsolutePath());
    zConf.setProperty(ZeppelinConfiguration.ConfVars.ZEPPELIN_NOTEBOOK_DIR.getVarName(),
        notebookDir.getAbsolutePath());
    zConf.setProperty(ZeppelinConfiguration.ConfVars.ZEPPELIN_INTERPRETER_GROUP_DEFAULT.getVarName(),
        "test");


    NotebookRepo notebookRepo = new InMemoryNotebookRepo();
    NoteManager noteManager = new NoteManager(notebookRepo, zConf);
    noteParser = new GsonNoteParser(zConf);
    storage = ConfigStorage.createConfigStorage(zConf);
    pluginManager = new PluginManager(zConf);
    AuthorizationService authorizationService =
        new AuthorizationService(noteManager, zConf, storage);

    interpreterSettingManager = new InterpreterSettingManager(zConf,
        mock(AngularObjectRegistryListener.class), mock(RemoteInterpreterProcessListener.class),
        mock(ApplicationEventListener.class), storage, pluginManager);
    interpreterFactory = new InterpreterFactory(interpreterSettingManager);
    Credentials credentials = new Credentials(zConf, storage);
    notebook = new Notebook(zConf, authorizationService, notebookRepo, noteManager, interpreterFactory, interpreterSettingManager, credentials);
    interpreterSettingManager.setNotebook(notebook);
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (interpreterSettingManager != null) {
      interpreterSettingManager.close();
    }
    if (interpreterDir != null) {
      LOGGER.info("Delete interpreterDir: {}", interpreterDir);
      FileUtils.deleteDirectory(interpreterDir);
    }
    if (confDir != null) {
      LOGGER.info("Delete confDir: {}", confDir);
      FileUtils.deleteDirectory(confDir);
    }
    if (notebookDir != null) {
      LOGGER.info("Delete notebookDir: {}", notebookDir);
      FileUtils.deleteDirectory(notebookDir);
    }
  }

  protected Note createNote() {
    return new Note("test", "test", interpreterFactory, interpreterSettingManager, null, null, null,
            zConf, noteParser);
  }

  protected InterpreterContext createDummyInterpreterContext() {
    return InterpreterContext.builder()
        .setNoteId("noteId")
        .setParagraphId("paragraphId")
        .build();
  }
}
