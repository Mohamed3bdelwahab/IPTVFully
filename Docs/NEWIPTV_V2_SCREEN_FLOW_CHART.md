# 📊 **NewIPTV V2 Screen Flow Chart**

## 🎯 **Navigation Flow Overview**

```
┌─────────────────────────────────────────────────────────────────┐
│                    NewIPTV V2 App Flow                        │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                    🏠 HOME SCREEN                              │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐   │
│  │ Series  │ │ Movies  │ │ Live TV │ │Settings │ │History  │   │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘   │
│  ┌─────────┐                                                   │
│  │  Fav    │                                                   │
│  └─────────┘                                                   │
└─────────────────────────────────────────────────────────────────┘
    │         │         │         │         │         │
    ▼         ▼         ▼         ▼         ▼         ▼
┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
│ Series  │ │ Movies  │ │ Live TV │ │Settings │ │History  │ │Favorites│
│ Screen  │ │ Screen  │ │ Screen  │ │ Screen  │ │ Screen  │ │ Screen  │
└─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│                    📺 SERIES SCREEN                            │
├─────────────────────┬───────────────────────────────────────────┤
│   Left Panel        │           Right Panel                    │
│   (Categories)      │           (Series List)                  │
│                     │                                           │
│ ┌─────────────────┐ │ ┌─────────────────────────────────────┐   │
│ │ 📁 Action       │ │ │ 🎬 Series Grid/List                 │   │
│ │ 📁 Comedy       │ │ │                                     │   │
│ │ 📁 Drama        │ │ │ ┌─────────┐ ┌─────────┐ ┌─────────┐ │   │
│ │ 📁 Horror       │ │ │ │Series 1 │ │Series 2 │ │Series 3 │ │   │
│ │ 📁 Sci-Fi       │ │ │ │         │ │         │ │         │ │   │
│ │ 📁 Thriller     │ │ │ └─────────┘ └─────────┘ └─────────┘ │   │
│ │ 📁 Romance      │ │ │ ┌─────────┐ ┌─────────┐ ┌─────────┐ │   │
│ │ 📁 Documentary  │ │ │ │Series 4 │ │Series 5 │ │Series 6 │ │   │
│ │ 📁 Animation    │ │ │ │         │ │         │ │         │ │   │
│ └─────────────────┘ │ └─────────┘ └─────────┘ └─────────┘ │   │
└─────────────────────┴───────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                    📋 SERIES DETAIL SCREEN                     │
├─────────────────────────────────────────────────────────────────┤
│  Series Title, Plot, Cast, Rating                              │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    Season Navigation                    │   │
│  │  [Season 1] [Season 2] [Season 3] [Season 4]           │   │
│  └─────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    Episode List                         │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐       │   │
│  │  │Ep 1     │ │Ep 2     │ │Ep 3     │ │Ep 4     │       │   │
│  │  │Title    │ │Title    │ │Title    │ │Title    │       │   │
│  │  │Duration │ │Duration │ │Duration │ │Duration │       │   │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘       │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                    🎬 VIDEO PLAYER SCREEN                      │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    Video Player                         │   │
│  │  ┌─────────────────────────────────────────────────┐   │   │
│  │  │                                                 │   │   │
│  │  │              Video Content                      │   │   │
│  │  │                                                 │   │   │
│  │  │                                                 │   │   │
│  │  └─────────────────────────────────────────────────┘   │   │
│  │  ┌─────────────────────────────────────────────────┐   │   │
│  │  │              Player Controls                     │   │   │
│  │  │  [⏮️] [⏯️] [⏭️] [🔊] [⚙️] [📺] [⏩] [⏪]        │   │   │
│  │  └─────────────────────────────────────────────────┘   │   │
│  │  ┌─────────────────────────────────────────────────┐   │   │
│  │  │              Progress Bar                        │   │   │
│  │  │  ████████████████████████████████████████████████   │   │   │
│  │  └─────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              TV Remote Controls                         │   │
│  │  [◀️] [▶️] [⏪] [⏩] [+/-] [0-6] [INFO] [MENU] [BACK]   │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## 🔄 **Detailed Navigation Paths**

### **1. Series Flow**
```
Home → Series → Category Selection → Series List → Series Detail → Episode Selection → Video Player
```

### **2. Movies Flow**
```
Home → Movies → Category Selection → Movie List → Movie Detail → Video Player
```

### **3. Live TV Flow**
```
Home → Live TV → Channel Category → Channel List → Live Stream
```

### **4. Settings Flow**
```
Home → Settings → Account Settings / Player Settings / App Settings
```

### **5. History Flow**
```
Home → History → Watch History List → Resume Content
```

### **6. Favorites Flow**
```
Home → Favorites → Favorite Content List → Play Content
```

## 📱 **Screen States**

### **Loading States**
- **Home Screen**: App initialization
- **Series Screen**: Loading categories/series
- **Video Player**: Buffering content
- **Settings**: Loading user preferences

### **Error States**
- **Network Error**: Connection issues
- **Content Error**: Video loading failures
- **Authentication Error**: Login issues
- **API Error**: Server communication issues

### **Success States**
- **Content Loaded**: Series/movies displayed
- **Video Playing**: Smooth playback
- **Settings Saved**: Preferences updated
- **Favorites Updated**: Content bookmarked

## 🎮 **TV Remote Navigation**

### **D-Pad Navigation**
```
    [UP]
[LEFT] [RIGHT]
   [DOWN]
   [CENTER]
```

### **Key Mappings**
- **D-Pad**: Navigate between items
- **Center/Enter**: Select item
- **Back**: Go back/Exit
- **Menu**: Show options
- **Info**: Show details
- **Numbers 0-6**: Speed presets
- **+/-**: Volume control
- **Media Keys**: Playback control

## 📊 **Data Flow**

### **API Calls**
```
1. App Start → Authentication
2. Home → Load user preferences
3. Series → Load categories → Load series
4. Series Detail → Load episodes
5. Video Player → Load stream URL
6. Settings → Save preferences
7. History → Load watch history
8. Favorites → Load favorite content
```

### **State Management**
```
ViewModel → Repository → API Service → Database
    ↓
UI Components ← State Flow ← ViewModel
```

## 🎯 **User Journey Examples**

### **Example 1: Watch a Series**
```
1. User opens app → Home Screen
2. Clicks "Series" → Series Screen
3. Selects "Action" category → Series List loads
4. Clicks "Breaking Bad" → Series Detail Screen
5. Selects "Season 1" → Episode list loads
6. Clicks "Episode 1" → Video Player starts
7. Uses TV remote to control playback
8. Video ends → Returns to episode list
```

### **Example 2: Browse Movies**
```
1. User opens app → Home Screen
2. Clicks "Movies" → Movies Screen
3. Selects "Comedy" category → Movie List loads
4. Scrolls through movies → Movie thumbnails
5. Clicks movie → Movie Detail Screen
6. Clicks "Play" → Video Player starts
7. Movie plays → Full screen experience
```

### **Example 3: Live TV**
```
1. User opens app → Home Screen
2. Clicks "Live TV" → Live TV Screen
3. Selects "Sports" category → Channel List loads
4. Clicks channel → Live Stream starts
5. Uses TV remote to change channels
6. Watches live content → Real-time streaming
```

## 🔧 **Technical Implementation**

### **Navigation Components**
- **NavController**: Main navigation controller
- **NavGraph**: Navigation graph definition
- **Deep Links**: Direct content access
- **Back Stack**: Navigation history

### **State Management**
- **ViewModel**: Screen state management
- **StateFlow**: Reactive state updates
- **Repository**: Data access layer
- **Database**: Local data storage

### **UI Components**
- **Composable**: Jetpack Compose UI
- **LazyColumn/Grid**: Efficient list rendering
- **Card**: Content containers
- **BottomSheet**: Overlay content

---

**Chart Created**: December 2024  
**Status**: ✅ **COMPLETE**  
**Next**: Implementation phase

