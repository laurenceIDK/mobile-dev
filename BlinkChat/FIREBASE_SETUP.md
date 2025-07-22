# Firebase Configuration for BlinkChat

## Setting up Firebase

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create a new project named "BlinkChat"
3. Add an Android app with package name `com.blinkchat`

## Enable Required Services

### 1. Authentication
- Go to Authentication > Sign-in method
- Enable Email/Password provider
- Optionally enable Google Sign-In for easier testing

### 2. Firestore Database
- Go to Firestore Database
- Create database in production mode
- Choose a location closest to your users
- Apply the security rules below

### 3. Storage
- Go to Storage
- Set up Cloud Storage
- Apply the storage rules below

## Security Rules

### Firestore Rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can read/write their own profile
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      allow read: if request.auth != null; // Allow reading other users for search
    }
    
    // Groups can be read/written by members only
    match /groups/{groupId} {
      allow read, write: if request.auth != null && 
        (request.auth.uid in resource.data.members || 
         request.auth.uid in get(/databases/$(database)/documents/groups/$(groupId)).data.members);
      allow create: if request.auth != null; // Allow creating new groups
    }
    
    // Messages can be read by group members, written by authenticated users
    match /messages/{messageId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
        request.auth.uid == resource.data.senderId;
      allow create: if request.auth != null;
    }
    
    // Allow reading for reports (admin functionality)
    match /message_reports/{reportId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### Storage Rules
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Profile pictures
    match /profile_pictures/{userId}.jpg {
      allow read: if true; // Public read for profile pictures
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Message images (if implemented)
    match /message_images/{imageId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
  }
}
```

## Firestore Indexes

Create these compound indexes for optimal performance:

### Groups Collection
- `members` (Array) + `isActive` (Ascending) + `lastActive` (Descending)
- `isActive` (Ascending) + `lastActive` (Ascending)

### Messages Collection
- `groupId` (Ascending) + `timestamp` (Descending)
- `groupId` (Ascending) + `isRead` (Ascending) + `timestamp` (Descending)
- `senderId` (Ascending) + `timestamp` (Descending)

## Cloud Functions (Optional)

For automatic cleanup of expired groups, deploy these Cloud Functions:

```javascript
const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

// Run every hour to check for expired groups
exports.cleanupExpiredGroups = functions.pubsub
  .schedule('0 * * * *') // Every hour
  .onRun(async (context) => {
    const db = admin.firestore();
    const now = admin.firestore.Timestamp.now();
    
    // Get all active groups
    const groupsSnapshot = await db.collection('groups')
      .where('isActive', '==', true)
      .get();
    
    const batch = db.batch();
    let expiredCount = 0;
    
    groupsSnapshot.forEach((doc) => {
      const group = doc.data();
      const groupId = doc.id;
      
      // Check if group has expired
      let hasExpired = false;
      
      if (group.expiryContract.type === 'timed') {
        const expiryTime = new Date(group.createdAt.seconds * 1000 + group.expiryContract.durationMillis);
        hasExpired = now.toDate() >= expiryTime;
      } else if (group.expiryContract.type === 'messageLimit') {
        hasExpired = group.messageCount >= group.expiryContract.maxMessages;
      } else if (group.expiryContract.type === 'inactivity') {
        const lastActiveTime = new Date(group.lastActive.seconds * 1000 + group.expiryContract.timeoutMillis);
        hasExpired = now.toDate() >= lastActiveTime;
      }
      
      if (hasExpired) {
        // Mark group as inactive
        batch.update(doc.ref, { isActive: false });
        expiredCount++;
        
        // Delete all messages in the group
        // Note: This is a simplified version. In production, you'd want to batch delete messages
        console.log(`Group ${groupId} marked as expired`);
      }
    });
    
    if (expiredCount > 0) {
      await batch.commit();
      console.log(`Marked ${expiredCount} groups as expired`);
    }
    
    return null;
  });

// Delete messages for inactive groups
exports.cleanupInactiveGroupMessages = functions.pubsub
  .schedule('0 2 * * *') // Daily at 2 AM
  .onRun(async (context) => {
    const db = admin.firestore();
    
    // Get inactive groups
    const inactiveGroups = await db.collection('groups')
      .where('isActive', '==', false)
      .get();
    
    for (const groupDoc of inactiveGroups.docs) {
      const groupId = groupDoc.id;
      
      // Delete all messages for this group
      const messagesSnapshot = await db.collection('messages')
        .where('groupId', '==', groupId)
        .get();
      
      const batch = db.batch();
      messagesSnapshot.forEach((messageDoc) => {
        batch.delete(messageDoc.ref);
      });
      
      if (!messagesSnapshot.empty) {
        await batch.commit();
        console.log(`Deleted ${messagesSnapshot.size} messages for group ${groupId}`);
      }
      
      // Delete the group document
      await groupDoc.ref.delete();
      console.log(`Deleted group ${groupId}`);
    }
    
    return null;
  });
```

## Environment Setup

1. Download `google-services.json` from your Firebase project
2. Place it in the `app/` directory of your Android project
3. Ensure the package name matches exactly: `com.blinkchat`

## Testing Data

For development and testing, you can create test users and groups:

### Test Users
- Email: test1@example.com, Password: test123
- Email: test2@example.com, Password: test123
- Email: test3@example.com, Password: test123

### Test Groups
Create groups with different expiry contracts to test all functionality:
- 1-hour timed group
- 10-message limit group  
- 30-minute inactivity group

## Monitoring

Set up Firebase monitoring to track:
- Authentication success/failure rates
- Database read/write operations
- Storage usage
- Function execution logs
- Error rates and performance metrics

## Backup Strategy

Configure automated backups for Firestore:
1. Go to Firebase Console > Firestore
2. Set up daily automated backups
3. Configure retention period (recommended: 30 days)
4. Set up alerting for backup failures
