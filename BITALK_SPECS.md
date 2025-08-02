# Bitalk - BLE Topic Matching App Specification

## Project Overview

Bitalk is an Android app that uses Bluetooth Low Energy (BLE) to create a mesh network for topic-based user discovery. When users with common interests approach each other, the app notifies them of the match and displays basic profile information.

## Core Concept

- Users set preferred topics and a brief self-description
- App creates BLE mesh network broadcasting this information
- When users with common topics are nearby, both receive notifications
- UI shows nearby matches as physics-based bubbles sized by distance

## Technical Foundation

### Base Implementation
- Built on existing `bitchat-android` BLE infrastructure
- Simplified from chat app to discovery-only app
- Removes encryption/fragmentation for discoverability
- Maintains robust BLE connection management and power optimization

### Key Differences from bitchat-android
- **No encryption**: Topics and descriptions broadcast in plain text for discoverability
- **Simplified packets**: Lightweight data structure vs complex BitchatPacket
- **No messaging**: Pure discovery/notification system
- **Physics UI**: Bubble visualization vs text-based chat interface

## User Experience Flow

### Onboarding Process
1. **Topic Selection**: Choose from predefined list or add custom topics
2. **Self Description**: Enter brief description (e.g., "yellow tshirt and sunglasses")  
3. **Username Assignment**: Auto-generate random username like "anon123"
4. **Permissions**: Request all required permissions with explanations
   - Bluetooth (for mesh network)
   - Location (for BLE scanning, not GPS tracking)
   - Notifications (for match alerts)
   - Background usage (for continuous scanning)
5. **Cannot proceed until all permissions granted**

### Main Interface Layout
```
┌─────────────────────────────────────┐
│ ● Scanning: 3 nearby users     ⏸️   │ ← Status bar
├─────────────────────────────────────┤
│                                     │
│         [Bubble Physics Area]       │ ← Main content area
│              ┌─────────────┐        │   with floating user bubbles
│              │  alex_dev   │        │   sized by distance
│              │"red hoodie" │        │
│              │    ~8m      │        │
│              └─────────────┘        │
│                                     │
│  ┌─────────────────────────────┐    │
│  │        sarah_btc            │    │
│  │   "yellow shirt, glasses"   │    │
│  │          ~2m                │    │
│  └─────────────────────────────┘    │
├─────────────────────────────────────┤
│  🏷️ bitcoin  🏷️ nostr  🏷️ pizza ⚙️ │ ← User's topics + preferences
├─────────────────────────────────────┤
│           bitalk/anon123            │ ← Username (tappable)
└─────────────────────────────────────┘
```

## UI/UX Design Requirements

### Visual Design
- **Modern and airy interface**
- **Sans serif font with good readability**
- **Neutral gray palette** with **#e32a6d accent color**
- **Automatic light/dark theme** following OS settings

### Color Usage
- **#e32a6d** for:
  - Bubble borders
  - User descriptions in bubbles
  - Distance indicators
  - Selected topic chips
- **Neutral grays** for background and secondary text

### Component Details

#### Status Bar (Top)
- Show scanning status: "Scanning: X nearby users" or "Paused"
- Play/pause button on right to control scanning
- Gray background, good contrast text

#### Bubble Area (Main)
- **Bubble sizing**: Inversely proportional to distance (closer = bigger)
- **Bubble content**: Username, description, estimated distance in meters
- **Physics animations**: 
  - New bubbles appear by enlarging in correct position
  - Existing bubbles "pushed" by new ones with physics-like interactions
  - Departing bubbles shrink and disappear smoothly
- **Bubble stacking**: Start from bottom, fill upward
- **Scrolling**: Vertical scroll if bubbles exceed screen space
- **Interaction**: Tap bubble to show detailed modal

#### Topics Section (Bottom)
- Horizontal scrollable chips showing user's selected topics
- Gear icon ⚙️ on right opens preferences modal
- Visual indication of selected topics

#### Username Footer
- Format: "bitalk/username"
- Tappable to open username change modal

### Interaction Patterns

#### Notifications
- Trigger when new user with matching topic detected
- Show: matching topic, username, description
- Don't spam - reasonable rate limiting

#### Modals
1. **Topic Preferences**:
   - Toggle between partial/exact matching
   - Add/remove topics from predefined list or custom entry
   
2. **User Detail** (when tapping bubble):
   - Full username, description, topic list
   - Time since first detected
   - Estimated distance
   
3. **Username Change**:
   - Simple text input
   - Validate and update

## Technical Architecture

### Data Structures

#### UserProfile (Persistent)
```kotlin
data class UserProfile(
    val username: String,        // e.g., "anon123"
    val description: String,     // e.g., "yellow tshirt and sunglasses"  
    val topics: List<String>,    // e.g., ["bitcoin", "nostr", "pizza"]
    val exactMatchMode: Boolean = false  // Partial vs exact topic matching
)
```

#### TopicBroadcast (BLE Packet)
```kotlin
data class TopicBroadcast(
    val username: String,
    val description: String,
    val topics: List<String>,
    val timestamp: Long
)
```

#### NearbyUser (Runtime)
```kotlin
data class NearbyUser(
    val username: String,
    val description: String,
    val topics: List<String>,
    val rssi: Int,
    val estimatedDistance: Double,  // meters
    val matchingTopics: List<String>,
    val firstSeen: Long,
    val lastSeen: Long
)
```

### Core Components

#### BitalkMeshService
- Simplified version of BluetoothMeshService
- Broadcasts TopicBroadcast packets
- Receives and processes nearby user data
- No encryption/fragmentation needed

#### DistanceCalculator
- Converts RSSI values to estimated meters
- Accounts for signal strength variations
- Provides reasonable distance estimates for bubble sizing

#### TopicMatcher
- Handles partial vs exact matching logic
- Scores matches based on common topics
- Filters relevant nearby users

#### BubblePhysicsEngine
- Simple but effective animation system
- Handles bubble appearance/disappearance
- Physics-like interactions between bubbles
- Smooth transitions for size changes

#### UserManager
- Persistent storage for UserProfile
- Handles username generation
- Manages topic preferences

### Predefined Topic List (20-30 items)
```kotlin
val DEFAULT_TOPICS = listOf(
    "bitcoin", "nostr", "crypto", "ethereum",
    "pizza", "coffee", "food", "cooking",
    "music", "art", "photography", "design", 
    "tech", "programming", "android", "linux",
    "travel", "hiking", "fitness", "yoga",
    "gaming", "books", "movies", "anime",
    "startup", "investing", "trading",
    "weird-interests", "memes", "cats"
)
```

## Power and Performance Requirements

### Background Behavior
- **Continue scanning when backgrounded**
- **Not aggressive** - optimize for battery life
- Use existing power management from bitchat-android
- Respect Android's background limitations

### BLE Optimization
- Efficient packet structure
- Reasonable advertising intervals
- Power-aware scanning duty cycles
- Connection limit enforcement

### Distance Calculation
- **RSSI to meters conversion**
- Account for environmental factors
- Smooth distance updates for stable bubble sizing

### Matching Algorithm
- **Partial matching default**: "bitcoin" matches "bitcoin-core", "bitcoin-lightning"
- **Exact matching option**: Available via preferences
- **Multiple topic support**: Match on any common topic
- **Real-time matching**: Update as users come/go

## Persistence Requirements

### Data Persistence
- **Username**: Persists across app restarts
- **Topics**: User's selected topics saved
- **Description**: User's self-description saved
- **Preferences**: Exact/partial matching mode saved

### Privacy Considerations
- **No location data stored**: Only use location permission for BLE
- **No chat history**: Pure discovery app
- **Local data only**: No remote servers
- **User control**: Easy to change username/description

## Development Tasks

### Phase 1: Foundation
1. Create simplified data structures
2. Adapt BLE services for unencrypted broadcasting
3. Implement RSSI to distance conversion
4. Create basic UI layout

### Phase 2: Core Features  
1. Implement topic matching logic
2. Create bubble physics engine
3. Add user profile management
4. Implement notifications

### Phase 3: Polish
1. Animations and transitions
2. Modal dialogs
3. Preferences and settings
4. Background optimization testing

This specification provides a clear roadmap for implementing Bitalk as a focused, efficient BLE-based topic discovery app built on the solid foundation of the existing bitchat-android codebase.