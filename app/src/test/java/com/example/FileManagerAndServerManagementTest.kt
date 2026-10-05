package com.example

import com.example.data.local.entity.ServerProfileEntity
import com.example.utils.FileManagerHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileManagerAndServerManagementTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testFileManagerCreateReadWriteList() {
        val testDir = tempFolder.newFolder("mc_server")

        // 1. Create file
        val fileCreated = FileManagerHelper.createNewFile(testDir, "server.properties")
        assertTrue("File should be created", fileCreated)

        // 2. Write file
        val content = "server-port=25565\nmotd=Hello World\ndifficulty=hard\n"
        val written = FileManagerHelper.writeFileText(File(testDir, "server.properties"), content)
        assertTrue("File content should be written", written)

        // 3. Read file
        val readText = FileManagerHelper.readFileText(File(testDir, "server.properties"))
        assertEquals(content, readText)

        // 4. Create subfolder
        val folderCreated = FileManagerHelper.createNewFolder(testDir, "plugins")
        assertTrue("Subfolder should be created", folderCreated)

        // 5. List directory
        val items = FileManagerHelper.listDirectory(testDir)
        assertEquals(2, items.size)
        // Folders come first in listing
        assertTrue(items[0].isDirectory)
        assertEquals("plugins", items[0].name)
        assertFalse(items[1].isDirectory)
        assertEquals("server.properties", items[1].name)
        assertTrue(items[1].isEditableText)

        // 6. Rename item
        val renamed = FileManagerHelper.renameItem(File(testDir, "server.properties"), "server.properties.bak")
        assertTrue("Item should be renamed", renamed)
        assertTrue(File(testDir, "server.properties.bak").exists())
        assertFalse(File(testDir, "server.properties").exists())

        // 7. Delete item
        val deleted = FileManagerHelper.deleteItem(File(testDir, "server.properties.bak"))
        assertTrue("Item should be deleted", deleted)
        assertFalse(File(testDir, "server.properties.bak").exists())
    }

    @Test
    fun testServerProfileDirectoryResolution() {
        val serverDefault = ServerProfileEntity(
            id = 5L,
            name = "Survival Hub",
            customServerDirectory = null
        )

        // With custom directory
        val customPath = "/storage/emulated/0/Documents/PaperCustom"
        val serverCustom = serverDefault.copy(
            id = 10L,
            customServerDirectory = customPath
        )

        assertEquals(customPath, serverCustom.customServerDirectory)
    }

    @Test
    fun testEditableExtensionsRecognition() {
        val items = listOf("server.properties", "spigot.yml", "paper.yml", "eula.txt", "ops.json", "start.bat", "run.sh")
        items.forEach { filename ->
            val item = com.example.utils.ServerFileItem(
                name = filename,
                path = "/mock/$filename",
                isDirectory = false,
                sizeBytes = 100,
                lastModified = System.currentTimeMillis(),
                extension = filename.substringAfterLast('.', "")
            )
            assertTrue("Expected $filename to be editable text", item.isEditableText)
        }
    }
}
