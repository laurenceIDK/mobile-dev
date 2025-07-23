# Campfire - Self-Destructing Group Chat App

## Overview

Campfire is a modern Android application built with Kotlin and Jetpack Compose that provides self-destructing group chat functionality. Groups automatically expire based on configurable contracts, ensuring temporary and ephemeral communication.nkChat - Self-Destructing Group Chat App

## 📱 Overview

BlinkChat is a modern Android application built with Kotlin and Jetpack Compose that provides self-destructing group chat functionality. Groups automatically expire based on configurable contracts, ensuring temporary and ephemeral communication.

## 🔧 Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt/Dagger
- **Backend**: Firebase
  - Firestore (real-time database)
  - Firebase Auth (authentication)
  - Firebase Storage (profile pictures)
  - Firebase Cloud Functions (auto-cleanup)

## 🎯 Core Features

### 1. User Authentication
- Sign Up / Sign In / Sign Out
- Email/Password authentication
- Password reset functionality
- User profile management

### 2. Group Management
- Create groups with custom expiry contracts
- Join groups via 6-character codes
- Add/Remove members
- Admin privileges system
- Real-time group updates

### 3. Ephemeral Messaging
- Real-time messaging with Firebase
- Message read status tracking
- Optional self-destruct timers
- System messages for group events

### 4. Auto-Destructing Groups
Multiple expiry contract types:
- **Timed**: Group expires after fixed duration (1h, 6h, 24h, 1 week, custom)
- **Message Limit**: Group expires after N messages (50, 100, custom)
- **Inactivity**: Group expires after X hours of no activity (2h, custom)
- **Poll-Based**: Group deleted by member vote (optional)

### 5. Visual Feedback
- Countdown timers for group expiry
- Expiry warnings in group headers
- "Boom" animations for deletions
- Real-time status updates

## 📁 Project Structure

```
app/src/main/java/com/campfire/
├── data/
│   ├── model/             # Data models (User, Group, Message, ExpiryContract)
│   └── repository/        # Repository interfaces and Firebase implementations
├── domain/
│   └── usecase/           # Business logic use cases
├── presentation/
│   ├── ui/                # Compose screens and components
│   └── viewmodel/         # ViewModels for each screen
├── utils/                 # Utility classes and extensions
├── di/                    # Dependency injection modules
├── ui/theme/              # Compose theme configuration
├── CampfireApplication.kt
└── MainActivity.kt
```

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest version)
- Android SDK 24+ (Android 7.0)
- Firebase project with enabled services

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd Campfire
   ```

2. **Firebase Configuration**
   - Create a new Firebase project at [Firebase Console](https://console.firebase.google.com)
   - Enable Authentication (Email/Password)
   - Enable Firestore Database
   - Enable Storage
   - Download `google-services.json` and place it in `app/` directory

3. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on Device**
   ```bash
   ./gradlew installDebug
   ```

## 🧪 Firebase Setup

### Firestore Security Rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Groups collection
    match /groups/{groupId} {
      allow read, write: if request.auth != null && 
        request.auth.uid in resource.data.members;
    }
    
    // Messages collection
    match /messages/{messageId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### Storage Security Rules
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /profile_pictures/{userId}.jpg {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

## 👥 Team Development Guide

This project is designed for a team of 4 developers, with each member focusing on specific modules:

### Team Member A: User Management (Profile CRUD)
- **Files to work on**:
  - `data/model/User.kt`
  - `data/repository/UserRepository.kt` & implementation
  - `domain/usecase/UserUseCase.kt`
  - User profile UI components
  - Profile editing functionality

### Team Member B: Group Management (Group CRUD)
- **Files to work on**:
  - `data/model/Group.kt` & `ExpiryContract.kt`
  - `data/repository/GroupRepository.kt` & implementation
  - `domain/usecase/GroupUseCase.kt`
  - `presentation/viewmodel/GroupViewModel.kt`
  - Group creation and management UI

### Team Member C: Messaging System (Message CRUD)
- **Files to work on**:
  - `data/model/Message.kt`
  - `data/repository/MessageRepository.kt` & implementation
  - `domain/usecase/MessageUseCase.kt`
  - `presentation/viewmodel/ChatViewModel.kt`
  - Chat UI and real-time messaging

### Team Member D: Member Management (Add/Remove Members)
- **Files to work on**:
  - Member management functions in `GroupRepository.kt`
  - Admin privilege system
  - Member invitation/removal UI
  - Group join functionality

## 🏗️ Architecture Principles

### MVVM Pattern
- **Model**: Data classes and repository implementations
- **View**: Jetpack Compose UI components
- **ViewModel**: State management and business logic coordination

### Clean Architecture
- **Data Layer**: Firebase repositories and data models
- **Domain Layer**: Use cases containing business logic
- **Presentation Layer**: UI components and ViewModels

### Key Design Patterns
- Repository Pattern for data access abstraction
- Use Case Pattern for business logic encapsulation
- Observer Pattern for real-time updates
- Dependency Injection for loose coupling

## 🔒 Security Considerations

- All Firebase operations require authentication
- Users can only access groups they're members of
- Admin operations require privilege verification
- Input validation on all user-provided data
- Secure handling of join codes

## 🧪 Testing Strategy

### Unit Tests
- Repository implementations
- Use case business logic
- Utility functions
- Data model validations

### Integration Tests
- Firebase operations
- Authentication flows
- End-to-end group creation and messaging

### UI Tests
- Compose UI components
- Navigation flows
- User interaction scenarios

## 📈 Performance Optimizations

- Efficient Firestore queries with proper indexing
- Image compression for profile pictures
- Pagination for message loading
- Background processing for expired group cleanup
- Proper state management to prevent unnecessary recompositions

## 🚫 Limitations & Known Issues

- Maximum group size: 100 members
- Message length limit: 1000 characters
- Self-destruct timer minimum: 1 minute
- Join codes expire after 30 days of inactivity
- Requires active internet connection

## 🔮 Future Enhancements

- Push notifications for new messages
- Voice messages support
- Image/file sharing
- Group video calls
- Message encryption
- Dark/Light theme toggle
- Multiple language support
- Offline message synchronization

## 📞 Support

For technical issues or questions:
1. Check existing GitHub issues
2. Create a new issue with detailed description
3. Include device information and error logs

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

**Note**: This is a student project designed for learning mobile app development with modern Android technologies. It demonstrates best practices in Clean Architecture, MVVM pattern, and Firebase integration.
