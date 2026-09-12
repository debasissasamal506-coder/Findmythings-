package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.BoxEntity
import com.example.data.local.ItemEntity
import com.example.data.repository.BackupRepository
import com.example.data.repository.PreferencesRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  private lateinit var db: AppDatabase

  @Before
  fun createDb() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun closeDb() {
    db.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Find My Things", appName)
  }

  @Test
  fun `test item insertion and location hierarchy`() = runBlocking {
    val item = ItemEntity(
      name = "Passport",
      category = "Documents",
      mainLocation = "Home",
      room = "Bedroom",
      storage = "Almirah",
      specificLocation = "2nd Drawer",
      notes = "Original passport",
      favorite = true
    )

    val id = db.itemDao().insertItem(item)
    assertTrue(id > 0)

    val fetched = db.itemDao().getItemByIdDirect(id)
    assertNotNull(fetched)
    assertEquals("Passport", fetched?.name)
    assertEquals("Home → Bedroom → Almirah → 2nd Drawer", fetched?.formatLocationHierarchy())
  }

  @Test
  fun `test box creation and backup export`() = runBlocking {
    val box = BoxEntity(
      name = "Box A01",
      location = "Store Room Shelf 2",
      notes = "Winter items"
    )
    val boxId = db.boxDao().insertBox(box)
    assertTrue(boxId > 0)

    val backupRepo = BackupRepository(db)
    val json = backupRepo.exportBackupJson()
    assertTrue(json.contains("Box A01"))
    assertTrue(json.contains("Find My Things"))
  }

  @Test
  fun `test item with voice note instructions and duration`() = runBlocking {
    val itemWithVoice = ItemEntity(
      name = "Car Keys",
      category = "Accessories",
      mainLocation = "Home",
      room = "Living Room",
      storage = "Key Bowl",
      specificLocation = "Beside Front Door",
      notes = null,
      audioUri = "file:///data/user/0/com.example/files/voice_notes/voice_12345.m4a",
      audioDurationSec = 14
    )

    val id = db.itemDao().insertItem(itemWithVoice)
    val fetched = db.itemDao().getItemByIdDirect(id)
    assertNotNull(fetched)
    assertTrue(fetched!!.hasVoiceNote)
    assertEquals("0:14", fetched.formatAudioDuration())
    assertEquals("file:///data/user/0/com.example/files/voice_notes/voice_12345.m4a", fetched.audioUri)

    // Test backup preservation of voice notes
    val backupRepo = BackupRepository(db)
    val json = backupRepo.exportBackupJson()
    assertTrue(json.contains("voice_12345.m4a"))
    assertTrue(json.contains("\"audioDurationSec\": 14"))
  }

  @Test
  fun `test mobile number login session persistence and account restore`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefsRepo = PreferencesRepository(context)

    // Initially not logged in
    assertFalse(prefsRepo.isLoggedIn.value)
    assertNull(prefsRepo.userPhoneNumber.value)

    // User logs in with mobile number
    val testPhoneNumber = "+91 98765 43210"
    val testUid = "user_919876543210"
    prefsRepo.setUserSession(testPhoneNumber, testUid)

    assertTrue(prefsRepo.isLoggedIn.value)
    assertEquals(testPhoneNumber, prefsRepo.userPhoneNumber.value)
    assertEquals(testUid, prefsRepo.userUid.value)

    // Verify last sync timestamp updates
    val syncTime = 1718000000000L
    prefsRepo.setLastSyncTimestamp(syncTime)
    assertEquals(syncTime, prefsRepo.lastSyncTimestamp.value)

    // Simulate account data restore from backup JSON
    val backupRepo = BackupRepository(db)
    val payload = """
      {
        "appName": "Find My Things",
        "version": 1,
        "boxes": [
          {"id": 101, "name": "Bedroom Wardrobe", "location": "Master Bedroom", "notes": "Upper shelf"}
        ],
        "items": [
          {
            "id": 201,
            "name": "Passport & Visa",
            "category": "Documents",
            "mainLocation": "Home",
            "room": "Master Bedroom",
            "storage": "Wardrobe",
            "specificLocation": "Top shelf Box 101",
            "boxId": 101,
            "favorite": true
          }
        ]
      }
    """.trimIndent()

    val restoreResult = backupRepo.restoreFromJson(payload)
    assertTrue(restoreResult is BackupRepository.RestoreResult.Success)
    val success = restoreResult as BackupRepository.RestoreResult.Success
    assertEquals(1, success.itemsRestored)
    assertEquals(1, success.boxesRestored)

    // Verify restored items can be queried
    val restoredItem = db.itemDao().getItemByIdDirect(201)
    assertNotNull(restoredItem)
    assertEquals("Passport & Visa", restoredItem!!.name)
    assertEquals(101L, restoredItem.boxId)
    assertTrue(restoredItem.favorite)

    // User signs out
    prefsRepo.clearUserSession()
    assertFalse(prefsRepo.isLoggedIn.value)
    assertNull(prefsRepo.userPhoneNumber.value)
    assertEquals(0L, prefsRepo.lastSyncTimestamp.value)
  }
}

