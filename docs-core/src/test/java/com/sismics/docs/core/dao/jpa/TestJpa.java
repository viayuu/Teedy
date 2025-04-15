package com.sismics.docs.core.dao.jpa;

import com.sismics.docs.BaseTransactionalTest;
import com.sismics.docs.core.dao.UserDao;
import com.sismics.docs.core.dao.criteria.UserCriteria;
import com.sismics.docs.core.dao.dto.UserDto;
import com.sismics.docs.core.model.jpa.User;
import com.sismics.docs.core.util.TransactionUtil;
import com.sismics.docs.core.util.authentication.InternalAuthenticationHandler;
import com.sismics.docs.core.util.jpa.SortCriteria;
import org.junit.Assert;
import org.junit.Test;

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
        user.setPassword("$2a$10$ztbqLXOA2PF7QeVzWLYLt.HbUvJQkLZ9OG8xCTP3XdGb.HpJwm4Xq"); // pre-hashed "differentpassword"
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
        Assert.assertTrue(globalStorage >= 3000L); // Should be at least 1000 + 2000 from our two test users
        
        // Test active user count
        long activeUserCount = userDao.getActiveUserCount();
        Assert.assertTrue(activeUserCount >= 2); // Should include our two test users
        
        // Test different hashPassword scenarios - can't directly test but we can verify behavior by authentication
        // Already tested normal password hashing above
        
        // Try to create a user with existing username (should throw exception)
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
}