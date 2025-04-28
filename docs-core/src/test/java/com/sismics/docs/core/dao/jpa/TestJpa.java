package com.sismics.docs.core.dao.jpa;

import com.sismics.docs.BaseTransactionalTest;
import com.sismics.docs.core.dao.DocumentDao;
import com.sismics.docs.core.dao.UserDao;
import com.sismics.docs.core.dao.criteria.UserCriteria;
import com.sismics.docs.core.dao.dto.DocumentDto;
import com.sismics.docs.core.dao.dto.UserDto;
import com.sismics.docs.core.model.jpa.Document;
import com.sismics.docs.core.model.jpa.User;
import com.sismics.docs.core.util.TransactionUtil;
import com.sismics.docs.core.util.authentication.InternalAuthenticationHandler;
import com.sismics.docs.core.util.jpa.SortCriteria;
import com.sismics.docs.core.constant.PermType;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Tests the persistance layer.
 * 
 * @author jtremeaux
 */
public class TestJpa extends BaseTransactionalTest {
    @Test
    public void testJpa() throws Exception {

        // Create a user
        UserDao userDao = new UserDao();
        User user = createUser("testJpa");

        TransactionUtil.commit();

        // Search a user by his ID
        user = userDao.getById(user.getId());
        Assert.assertNotNull(user);
        Assert.assertEquals("toto@docs.com", user.getEmail());

        // Authenticate using the database
        Assert.assertNotNull(new InternalAuthenticationHandler().authenticate("testJpa", "12345678"));

        // Test invalid authentication
        Assert.assertNull(new InternalAuthenticationHandler().authenticate("testJpa", "wrongpassword"));
        Assert.assertNull(new InternalAuthenticationHandler().authenticate("nonexistinguser", "12345678"));

        // Test getActiveByUsername
        User activeUser = userDao.getActiveByUsername("testJpa");
        Assert.assertNotNull(activeUser);
        Assert.assertEquals(user.getId(), activeUser.getId());
        Assert.assertEquals(user.getEmail(), activeUser.getEmail());
        
        // Test update user
        user.setEmail("updated@docs.com");
        User updatedUser = userDao.update(user, "testJpa");
        TransactionUtil.commit();
        User checkUser = userDao.getById(user.getId());
        Assert.assertEquals("updated@docs.com", checkUser.getEmail());
        
        // Test update password
        user.setPassword("newpassword");
        userDao.updatePassword(user, "testJpa");
        TransactionUtil.commit();
        Assert.assertNotNull(new InternalAuthenticationHandler().authenticate("testJpa", "newpassword"));
        Assert.assertNull(new InternalAuthenticationHandler().authenticate("testJpa", "12345678"));
        
        // Test storage quota
        Assert.assertEquals((long)100_000L, (long)user.getStorageQuota());
        user.setStorageCurrent(1000L);
        userDao.updateQuota(user);
        TransactionUtil.commit();
        User quotaUser = userDao.getById(user.getId());
        Assert.assertEquals((long)1000L, (long)quotaUser.getStorageCurrent());
        
        // Create another user for testing multiple user scenarios
        User user2 = new User();
        user2.setUsername("testJpa2");
        user2.setPassword("12345678");
        user2.setEmail("toto2@docs.com");
        user2.setRoleId("user");
        user2.setStorageQuota(200_000L);
        userDao.create(user2, "testJpa");
        TransactionUtil.commit();
        
        // Set some storage for the second user to test global storage
        user2.setStorageCurrent(2000L);
        userDao.updateQuota(user2);
        TransactionUtil.commit();
        
        // Test finding users by criteria
        UserCriteria criteria = new UserCriteria();
        criteria.setSearch("test");
        SortCriteria sortCriteria = new SortCriteria(1, true);
        
        List<UserDto> users = userDao.findByCriteria(criteria, sortCriteria);
        Assert.assertTrue(users.size() >= 2); // Should find at least our two test users
        
        // Test with username criteria
        criteria = new UserCriteria();
        criteria.setUserName("testJpa");
        users = userDao.findByCriteria(criteria, sortCriteria);
        Assert.assertEquals(1, users.size());
        Assert.assertEquals("testJpa", users.get(0).getUsername());
        
        // Test with userId criteria
        criteria = new UserCriteria();
        criteria.setUserId(user.getId());
        users = userDao.findByCriteria(criteria, sortCriteria);
        Assert.assertEquals(1, users.size());
        Assert.assertEquals(user.getId(), users.get(0).getId());
        
        // Test with no criteria (should return all users)
        criteria = new UserCriteria();
        users = userDao.findByCriteria(criteria, sortCriteria);
        Assert.assertTrue(users.size() >= 2);
        
        // Test sorting in different order
        sortCriteria = new SortCriteria(1, false); // Sort by username descending
        users = userDao.findByCriteria(criteria, sortCriteria);
        Assert.assertTrue(users.size() >= 2);
        
        // Test sorting by different column
        sortCriteria = new SortCriteria(2, true); // Sort by email ascending
        users = userDao.findByCriteria(criteria, sortCriteria);
        Assert.assertTrue(users.size() >= 2);
        
        // Test update onboarding status
        user.setOnboarding(true);
        userDao.updateOnboarding(user);
        TransactionUtil.commit();
        User onboardingUser = userDao.getById(user.getId());
        Assert.assertTrue(onboardingUser.isOnboarding());
        
        // Test update to false
        user.setOnboarding(false);
        userDao.updateOnboarding(user);
        TransactionUtil.commit();
        onboardingUser = userDao.getById(user.getId());
        Assert.assertFalse(onboardingUser.isOnboarding());
        
        // Test update hashed password directly
        String originalPassword = user.getPassword();
        user.setPassword("$2a$10$ztbqLXOA2PF7QeVzWLYLt.HbUvJQkLZ9OG8xCTP3XdGb.HpJwm4Xq"); 
        userDao.updateHashedPassword(user);
        TransactionUtil.commit();
        Assert.assertNotNull(new InternalAuthenticationHandler().authenticate("testJpa", "differentpassword"));
        
        // Test updating TOTP key
        user.setTotpKey("TESTTOTPKEY");
        userDao.update(user, "testJpa");
        TransactionUtil.commit();
        User totpUser = userDao.getById(user.getId());
        Assert.assertEquals("TESTTOTPKEY", totpUser.getTotpKey());
        
        // Test disabling a user
        user.setDisableDate(new Date());
        userDao.update(user, "testJpa");
        TransactionUtil.commit();
        Assert.assertNull(new InternalAuthenticationHandler().authenticate("testJpa", "differentpassword"));
        
        // Enable the user again for further tests
        user.setDisableDate(null);
        userDao.update(user, "testJpa");
        TransactionUtil.commit();
        
        // Test global storage calculations
        long globalStorage = userDao.getGlobalStorageCurrent();
        Assert.assertTrue(globalStorage >= 3000L);
        
        // Test active user count
        long activeUserCount = userDao.getActiveUserCount();
        Assert.assertTrue(activeUserCount >= 2);
        
        
        // Try to create a user with existing username
        User duplicateUser = new User();
        duplicateUser.setUsername("testJpa");
        duplicateUser.setPassword("password");
        duplicateUser.setEmail("another@docs.com");
        duplicateUser.setRoleId("user");
        boolean exceptionThrown = false;
        try {
            userDao.create(duplicateUser, "testJpa");
        } catch (Exception e) {
            exceptionThrown = true;
            Assert.assertEquals("AlreadyExistingUsername", e.getMessage());
        }
        Assert.assertTrue(exceptionThrown);
        
        // Test getById with invalid ID
        User nonExistentUser = userDao.getById("nonexistentid");
        Assert.assertNull(nonExistentUser);
        
        // Test user search
        Assert.assertNull(userDao.getActiveByUsername("nonexistinguser"));
        
        // Test case with deleted user
        userDao.delete("testJpa2", user.getId());
        TransactionUtil.commit();
        Assert.assertNull(userDao.getActiveByUsername("testJpa2"));
        
        // Delete the first user
        userDao.delete("testJpa", user.getId());
        TransactionUtil.commit();
        
        // Verify user was deleted
        Assert.assertNull(userDao.getActiveByUsername("testJpa"));
        
        // Global storage should be reduced after user deletion
        long newGlobalStorage = userDao.getGlobalStorageCurrent();
        Assert.assertTrue(newGlobalStorage < globalStorage);
    }
    
    /**
     * Test the DocumentDao class.
     */
    @Test
    public void testDocumentDao() throws Exception {
        // Create test users for document operations
        UserDao userDao = new UserDao();
        User user1 = createUser("docUser1");
        TransactionUtil.commit();
        
        User user2 = new User();
        user2.setUsername("docUser2");
        user2.setPassword("12345678");
        user2.setEmail("docuser2@docs.com");
        user2.setRoleId("user");
        user2.setStorageQuota(200_000L);
        userDao.create(user2, "docUser1");
        TransactionUtil.commit();
        
        // Initialize DocumentDao
        DocumentDao documentDao = new DocumentDao();
        
        // Test getDocumentCount before adding any documents
        long initialDocCount = documentDao.getDocumentCount();
        
        // Create a document with all metadata fields
        Document document1 = new Document();
        document1.setUserId(user1.getId());
        document1.setTitle("Test Document 1");
        document1.setDescription("Test Description 1");
        document1.setLanguage("eng");
        document1.setCreateDate(new Date());
        document1.setSubject("Test Subject");
        document1.setIdentifier("DOC-ID-001");
        document1.setPublisher("Test Publisher");
        document1.setFormat("Test Format");
        document1.setSource("Test Source");
        document1.setType("Test Type");
        document1.setCoverage("Test Coverage");
        document1.setRights("Test Rights");
        
        String document1Id = documentDao.create(document1, user1.getId());
        TransactionUtil.commit();
        Assert.assertNotNull(document1Id);
        
        // Test getById
        Document fetchedDocument = documentDao.getById(document1Id);
        Assert.assertNotNull(fetchedDocument);
        Assert.assertEquals("Test Document 1", fetchedDocument.getTitle());
        Assert.assertEquals("Test Description 1", fetchedDocument.getDescription());
        Assert.assertEquals("eng", fetchedDocument.getLanguage());
        Assert.assertEquals("Test Subject", fetchedDocument.getSubject());
        Assert.assertEquals("DOC-ID-001", fetchedDocument.getIdentifier());
        Assert.assertEquals("Test Publisher", fetchedDocument.getPublisher());
        Assert.assertEquals("Test Format", fetchedDocument.getFormat());
        Assert.assertEquals("Test Source", fetchedDocument.getSource());
        Assert.assertEquals("Test Type", fetchedDocument.getType());
        Assert.assertEquals("Test Coverage", fetchedDocument.getCoverage());
        Assert.assertEquals("Test Rights", fetchedDocument.getRights());
        Assert.assertEquals(user1.getId(), fetchedDocument.getUserId());
        
        // Create a second document with minimal fields
        Document document2 = new Document();
        document2.setUserId(user2.getId());
        document2.setTitle("Test Document 2");
        document2.setLanguage("fra");
        document2.setCreateDate(new Date());
        
        String document2Id = documentDao.create(document2, user2.getId());
        TransactionUtil.commit();
        Assert.assertNotNull(document2Id);
        
        // Test getDocumentCount after adding documents
        long newDocCount = documentDao.getDocumentCount();
        Assert.assertEquals(initialDocCount + 2, newDocCount);
        
        // Test findAll
        List<Document> allDocs = documentDao.findAll(0, 10);
        Assert.assertTrue(allDocs.size() >= 2);
        boolean foundDoc1 = false;
        boolean foundDoc2 = false;
        for (Document doc : allDocs) {
            if (doc.getId().equals(document1Id)) {
                foundDoc1 = true;
            }
            if (doc.getId().equals(document2Id)) {
                foundDoc2 = true;
            }
        }
        Assert.assertTrue(foundDoc1);
        Assert.assertTrue(foundDoc2);
        
        // Test findAll with pagination
        List<Document> firstPage = documentDao.findAll(0, 1);
        Assert.assertEquals(1, firstPage.size());
        List<Document> secondPage = documentDao.findAll(1, 1);
        Assert.assertEquals(1, secondPage.size());
        Assert.assertNotEquals(firstPage.get(0).getId(), secondPage.get(0).getId());
        
        // Test findByUserId for first user
        List<Document> user1Docs = documentDao.findByUserId(user1.getId());
        boolean foundUser1Doc = false;
        for (Document doc : user1Docs) {
            if (doc.getId().equals(document1Id)) {
                foundUser1Doc = true;
                break;
            }
        }
        Assert.assertTrue(foundUser1Doc);
        
        // Test findByUserId for second user
        List<Document> user2Docs = documentDao.findByUserId(user2.getId());
        boolean foundUser2Doc = false;
        for (Document doc : user2Docs) {
            if (doc.getId().equals(document2Id)) {
                foundUser2Doc = true;
                break;
            }
        }
        Assert.assertTrue(foundUser2Doc);
        
        // Test getDocument with permissions
        List<String> adminTargetList = new ArrayList<>();
        adminTargetList.add("admin");
        DocumentDto docDto = documentDao.getDocument(document1Id, PermType.READ, adminTargetList);
        
        // Test update document
        document1.setTitle("Updated Document Title");
        document1.setDescription("Updated Description");
        document1.setLanguage("spa");
        Document updatedDoc = documentDao.update(document1, user1.getId());
        TransactionUtil.commit();
        
        Document checkDoc = documentDao.getById(document1Id);
        Assert.assertEquals("Updated Document Title", checkDoc.getTitle());
        Assert.assertEquals("Updated Description", checkDoc.getDescription());
        Assert.assertEquals("spa", checkDoc.getLanguage());
        
        // Test updateFileId    
        document1.setFileId("testfileid456");
        documentDao.updateFileId(document1);
        TransactionUtil.commit();

        Document fileIdDoc = documentDao.getById(document1Id);
        Assert.assertEquals("testfileid456", fileIdDoc.getFileId());
        
        // Test delete document 
        documentDao.delete(document1Id, user1.getId());
        TransactionUtil.commit();   
        
        // Verify document was deleted
        Document deletedDoc = documentDao.getById(document1Id);
        Assert.assertNull(deletedDoc);  
        
        // Verify document count decreased
        long afterDeleteCount = documentDao.getDocumentCount(); 
        Assert.assertEquals(initialDocCount + 1, afterDeleteCount);
        
        // Test getById with nonexistent document
        Document nonExistentDoc = documentDao.getById("nonexistentid");
        Assert.assertNull(nonExistentDoc);      

        // Test getDocument with nonexistent document
        DocumentDto nonExistentDocDto = documentDao.getDocument("nonexistentid", PermType.READ, adminTargetList);
        Assert.assertNull(nonExistentDocDto);   
        
        // Delete the second document
        documentDao.delete(document2Id, user2.getId ());
        TransactionUtil.commit();
        
        // Clean up - delete test users
        userDao.delete("docUser2", user1.getId());  
        TransactionUtil.commit();
        userDao.delete("docUser1", user1.getId());
        TransactionUtil.commit();
    }
}