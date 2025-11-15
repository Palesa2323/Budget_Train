# Firestore Security Rules Setup

## Quick Setup (For Development/Testing)

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Navigate to **Firestore Database** → **Rules** tab
4. Replace the default rules with the following:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Helper function to get current user's hashCode as string
    function getUserHashCode() {
      return request.auth.uid.hashCode().toString();
    }
    
    // Users can only read/write their own user document
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Expenses: users can only read/write their own expenses
    // Note: We're storing userId as hashCode string, so we need to check that
    match /expenses/{expenseId} {
      allow read, write: if request.auth != null && 
        resource.data.userId == request.auth.uid.hashCode().toString();
      allow create: if request.auth != null && 
        request.resource.data.userId == request.auth.uid.hashCode().toString();
    }
    
    // Categories: users can only read/write their own categories
    match /categories/{categoryId} {
      allow read, write: if request.auth != null && 
        resource.data.userId == request.auth.uid.hashCode().toString();
      allow create: if request.auth != null && 
        request.resource.data.userId == request.auth.uid.hashCode().toString();
    }
    
    // Budget goals: users can only read/write their own goals
    match /budget_goals/{goalId} {
      allow read, write: if request.auth != null && 
        resource.data.userId == request.auth.uid.hashCode().toString();
      allow create: if request.auth != null && 
        request.resource.data.userId == request.auth.uid.hashCode().toString();
    }
  }
}
```

**IMPORTANT**: Firestore security rules don't support `hashCode()` function! The above won't work.

## Working Solution (Temporary - For Testing Only)

For now, use these rules to allow authenticated users to access their data. **This is only for development/testing**:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow authenticated users to read/write all data
    // WARNING: This is for development only! Not secure for production.
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

## Better Long-term Solution

The current approach of using `hashCode()` is problematic because:
1. Firestore security rules can't compute hashCode
2. HashCode can have collisions
3. It's harder to maintain

**Recommended**: Store the Firebase UID directly as a string in Firestore instead of converting to hashCode. This would require code changes but would be more secure and maintainable.

## Steps to Apply Rules

1. Copy one of the rule sets above (use the "Working Solution" for now)
2. Go to Firebase Console → Firestore Database → Rules
3. Paste the rules
4. Click **Publish**
5. Wait a few seconds for rules to propagate
6. Test your app again

After applying the rules, your app should be able to read and write data to Firestore.
