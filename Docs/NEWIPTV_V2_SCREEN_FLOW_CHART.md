# 📱 **NewIPTV V2 Screen Flow Chart**

## 🎯 **Main Navigation Flow**

```
┌─────────────────────────────────────────────────────────────┐
│                    NewIPTV V2 Home Screen                   │
│                                                             │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ │
│  │ Series  │ │ Movies  │ │ Live TV │ │Settings │ │History  │ │
│  │         │ │         │ │         │ │         │ │         │ │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘ │
│  ┌─────────┐                                                 │
│  │  Fav    │                                                 │
│  │         │                                                 │
│  └─────────┘                                                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Series Screen                            │
├─────────────────────┬───────────────────────────────────────┤
│   Left Panel        │           Right Panel                 │
│   (Categories)      │           (Series List)               │
│                     │                                       │
│ ┌─────────────────┐ │ ┌─────────────────────────────────────┐ │
│ │ 📁 Action       │ │ │ 🎬 Series Grid/List                 │
│ │ 📁 Comedy       │ │ │                                     │
│ │ 📁 Drama        │ │ │ ┌─────────┐ ┌─────────┐ ┌─────────┐ │
│ │ 📁 Horror       │ │ │ │Series 1 │ │Series 2 │ │Series 3 │ │
│ │ 📁 Sci-Fi       │ │ │ │         │ │         │ │         │ │
│ │ 📁 Thriller     │ │ │ └─────────┘ └─────────┘ └─────────┘ │
│ │ 📁 Romance      │ │ │ ┌─────────┐ ┌─────────┐ ┌─────────┐ │
│ │ 📁 Documentary  │ │ │ │Series 4 │ │Series 5 │ │Series 6 │ │
│ │ 📁 Animation    │ │ │ │         │ │         │ │         │ │
│ └─────────────────┘ │ └─────────┘ └─────────┘ └─────────┘ │
│                     │                                     │
│                     │ ┌─────────────────────────────────────┐ │
│                     │ │         Search & Filter             │ │
│                     │ └─────────────────────────────────────┘ │
└─────────────────────┴───────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                 Series Detail Screen                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                    Series Info                          │ │
│  │  Title, Plot, Cast, Rating, etc.                        │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                   Episode List                          │ │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐         │ │
│  │  │Ep 1     │ │Ep 2     │ │Ep 3     │ │Ep 4     │         │ │
│  │  │         │ │         │ │         │ │         │         │ │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘         │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                 Video Player Screen                         │
│                                                             │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                                                         │ │
│  │                    Video Player                         │ │
│  │                                                         │ │
│  │                                                         │ │
│  │                                                         │ │
│  │                                                         │ │
│  │                                                         │ │
│  │                                                         │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                 TV Remote Controls                      │ │
│  │  Play/Pause | Seek | Speed | Episode Nav | Back        │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## 🔄 **Navigation Paths**

### **1. Series Flow**
```
Home → Series → Category Selection → Series List → Series Detail → Video Player
```

### **2. Movies Flow**
```
Home → Movies → Category Selection → Movie List → Movie Detail → Video Player
```

### **3. Live TV Flow**
```
Home → Live TV → Channel Categories → Channel List → Live Stream
```

### **4. User Data Flow**
```
Home → History → Watch History List
Home → Favorites → Favorite Content List
Home → Settings → App Configuration
```

## 📱 **Screen States**

### **Home Screen States**
- **Default**: Show 6 menu cards
- **Loading**: Show loading spinner
- **Error**: Show error message with retry

### **Series Screen States**
- **Loading Categories**: Show loading in left panel
- **Loading Series**: Show loading in right panel
- **Empty State**: Show "No series found" message
- **Error State**: Show error with retry button

### **Video Player States**
- **Loading**: Show loading spinner
- **Playing**: Show video with controls
- **Paused**: Show video with play button
- **Error**: Show error message
- **Buffering**: Show buffering indicator

## 🎮 **TV Remote Navigation**

### **Home Screen Navigation**
- **D-Pad**: Navigate between menu cards
- **Enter**: Select menu item
- **Back**: Exit app

### **Series Screen Navigation**
- **D-Pad Left/Right**: Switch between panels
- **D-Pad Up/Down**: Navigate within panel
- **Enter**: Select category or series
- **Back**: Return to home

### **Video Player Navigation**
- **D-Pad Center**: Play/Pause
- **D-Pad Left/Right**: Seek 10 seconds
- **Fast Forward/Rewind**: Seek 30 seconds
- **Number Keys**: Speed presets
- **Info**: Show speed menu
- **Back**: Return to series detail

## 📊 **Data Flow**

```
API Service → ViewModel → UI State → Screen Display
     ↓           ↓           ↓           ↓
IPTVApiService → SeriesViewModel → Loading/Success/Error → SeriesScreen
```

## 🎯 **Key Features Per Screen**

### **Home Screen**
- ✅ 6 main menu sections
- ✅ TV remote navigation
- ✅ Visual feedback on selection

### **Series Screen**
- ✅ Dual-panel layout
- ✅ Category filtering
- ✅ Series grid/list view
- ✅ Search functionality

### **Video Player Screen**
- ✅ Existing video player (NO CHANGES)
- ✅ TV remote controls
- ✅ Speed overlay menu
- ✅ Episode navigation

---

**Chart Created**: December 2024  
**Status**: 📋 **COMPLETE**  
**Note**: Video Player remains unchanged as requested

