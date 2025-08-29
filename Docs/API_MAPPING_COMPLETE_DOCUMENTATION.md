# 🌐 **API Mapping Complete Documentation - IPTV Android Application**

## 📋 **Overview**
This document provides comprehensive documentation for the API mapping system in the IPTV Android application, including all API services, endpoints, data models, and implementation details.

---

## 🏗️ **Libraries & Dependencies Used**

### **Core Networking Libraries**
```kotlin
// Retrofit - HTTP Client
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-moshi:2.9.0")

// OkHttp - HTTP Client
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Moshi - JSON Parsing
implementation("com.squareup.moshi:moshi:1.15.1")
implementation("com.squareup.moshi:moshi-kotlin:1.15.1")

// Dependency Injection
implementation("com.google.dagger:hilt-android:2.50")
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
```

---


## 🎬 **IPTVApiService - Series Content API**

### **API Templates & URL Building**
```kotlin
// API Templates from ImportanLinks.json
private const val API_BASE_TEMPLATE = "http://{server}:{port}/player_api.php"
private const val AUTH_QUERY_TEMPLATE = "username={username}&password={password}"
private const val WITH_ACTION_TEMPLATE = "{api_base}?{auth_query}&action={action}"

// Streaming Templates
private const val CDN_HOST = "http://aws85485.amazonedge.net/"
private const val BASE_PATH_TEMPLATE = "{cdn_host}/{mainScreen}/{username}/{password}/"

// API Endpoints
private const val GET_SERIES_CATEGORIES = "get_series_categories"
private const val GET_SERIES = "get_series"
private const val GET_SERIES_INFO = "get_series_info"
```

### **URL Builder Functions**
```kotlin
// Build API URL with authentication
private fun buildApiUrl(action: String, params: Map<String, String> = emptyMap()): String {
    val apiBase = API_BASE_TEMPLATE
        .replace("{server}", serverUrl)
        .replace("{port}", serverPort)
    
    val authQuery = AUTH_QUERY_TEMPLATE
        .replace("{username}", username)
        .replace("{password}", password)
    
    return if (params.isEmpty()) {
        WITH_ACTION_TEMPLATE
            .replace("{api_base}", apiBase)
            .replace("{auth_query}", authQuery)
            .replace("{action}", action)
    } else {
        val paramString = params.entries.joinToString("&") { "${it.key}=${it.value}" }
        "$apiBase?$authQuery&action=$action&$paramString"
    }
}

// Build streaming URL for episodes
fun buildStreamingUrl(episodeId: String, mainScreen: String = "series", extension: String = "mkv"): String {
    return BASE_PATH_TEMPLATE
        .replace("{cdn_host}", CDN_HOST)
        .replace("{mainScreen}", mainScreen)
        .replace("{username}", username)
        .replace("{password}", password) + "$episodeId.$extension"
}
```

### **API Endpoints Implementation**

#### **1. Authentication**
```kotlin
suspend fun authenticate(): ApiResponse<LoginResponse> {
    return withContext(Dispatchers.IO) {
        try {
            val url = buildApiUrl("")
            
            activityLogger.logApiCall(
                className = "IPTVApiService",
                methodName = "authenticate",
                url = url,
                parameters = mapOf("username" to username),
                lineNumber = 150
            )
            
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val jsonObject = JSONObject(responseBody)
                    
                    if (jsonObject.has("user_info")) {
                        val userInfo = parseUserInfo(jsonObject.getJSONObject("user_info"))
                        val serverInfo = if (jsonObject.has("server_info")) {
                            parseServerInfo(jsonObject.getJSONObject("server_info"))
                        } else null
                        
                        isAuthenticated = true
                        ApiResponse.Success(LoginResponse(userInfo, serverInfo))
                    } else {
                        ApiResponse(false, null, "Invalid response format")
                    }
                } else {
                    ApiResponse(false, null, "Empty response")
                }
            } else {
                ApiResponse(false, null, "HTTP ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Authentication failed", e)
            ApiResponse(false, null, e.message ?: "Unknown error")
        }
    }
}
```

#### **2. Get Series Categories**
```kotlin
suspend fun getSeriesCategories(): ApiResponse<List<SeriesCategory>> {
    return withContext(Dispatchers.IO) {
        try {
            if (!isAuthenticated) {
                val authResult = authenticate()
                if (!authResult.success) {
                    return@withContext ApiResponse(false, null, "Authentication failed")
                }
            }
            
            val url = buildApiUrl(GET_SERIES_CATEGORIES)
            
            activityLogger.logApiCall(
                className = "IPTVApiService",
                methodName = "getSeriesCategories",
                url = url,
                parameters = mapOf("action" to GET_SERIES_CATEGORIES),
                lineNumber = 210
            )
            
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val jsonArray = JSONArray(responseBody)
                    val categories = mutableListOf<SeriesCategory>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val category = jsonArray.getJSONObject(i)
                        categories.add(
                            SeriesCategory(
                                category_id = category.getString("category_id"),
                                category_name = category.getString("category_name"),
                                parent_id = category.optInt("parent_id", 0)
                            )
                        )
                    }
                    
                    ApiResponse.Success(categories)
                } else {
                    ApiResponse(false, null, "Empty response")
                }
            } else {
                ApiResponse(false, null, "HTTP ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get series categories", e)
            ApiResponse(false, null, e.message ?: "Unknown error")
        }
    }
}
```

#### **3. Get Series List**
```kotlin
suspend fun getSeries(categoryId: String = "*"): ApiResponse<List<Series>> {
    return withContext(Dispatchers.IO) {
        try {
            if (!isAuthenticated) {
                val authResult = authenticate()
                if (!authResult.success) {
                    return@withContext ApiResponse(false, null, "Authentication failed")
                }
            }
            
            val url = buildApiUrl(GET_SERIES, mapOf("category_id" to categoryId))
            
            activityLogger.logApiCall(
                className = "IPTVApiService",
                methodName = "getSeries",
                url = url,
                parameters = mapOf(
                    "action" to GET_SERIES,
                    "category_id" to categoryId
                ),
                lineNumber = 280
            )
            
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val jsonArray = JSONArray(responseBody)
                    val series = mutableListOf<Series>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val serie = jsonArray.getJSONObject(i)
                        series.add(
                            Series(
                                series_id = serie.getInt("series_id"),
                                name = serie.getString("name"),
                                cover = serie.optString("cover", ""),
                                plot = serie.optString("plot", ""),
                                cast = serie.optString("cast", ""),
                                director = serie.optString("director", ""),
                                genre = serie.optString("genre", ""),
                                releaseDate = serie.optString("releaseDate", ""),
                                rating = serie.optString("rating", ""),
                                rating_5based = serie.optDouble("rating_5based", 0.0),
                                category_id = serie.getString("category_id"),
                                backdrop_path = parseStringList(serie.optJSONArray("backdrop_path")),
                                youtube_trailer = serie.optString("youtube_trailer", ""),
                                episode_run_time = serie.optString("episode_run_time", "0")
                            )
                        )
                    }
                    
                    ApiResponse.Success(series)
                } else {
                    ApiResponse(false, null, "Empty response")
                }
            } else {
                ApiResponse(false, null, "HTTP ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get series", e)
            ApiResponse(false, null, e.message ?: "Unknown error")
        }
    }
}
```

#### **4. Get Series Info with Episodes**
```kotlin
suspend fun getSeriesInfo(seriesId: Int): ApiResponse<SeriesInfo> {
    return withContext(Dispatchers.IO) {
        try {
            if (!isAuthenticated) {
                val authResult = authenticate()
                if (!authResult.success) {
                    return@withContext ApiResponse(false, null, "Authentication failed")
                }
            }
            
            val url = buildApiUrl(GET_SERIES_INFO, mapOf("series_id" to seriesId.toString()))
            
            activityLogger.logApiCall(
                className = "IPTVApiService",
                methodName = "getSeriesInfo",
                url = url,
                parameters = mapOf(
                    "action" to GET_SERIES_INFO,
                    "series_id" to seriesId.toString()
                ),
                lineNumber = 360
            )
            
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val jsonObject = JSONObject(responseBody)
                    
                    if (jsonObject.has("info") && jsonObject.has("episodes")) {
                        val info = parseSeriesInfoDetails(jsonObject.getJSONObject("info"))
                        val episodes = parseEpisodes(jsonObject.getJSONObject("episodes"))
                        
                        ApiResponse.Success(SeriesInfo(info, episodes))
                    } else {
                        ApiResponse(false, null, "Invalid response format")
                    }
                } else {
                    ApiResponse(false, null, "Empty response")
                }
            } else {
                ApiResponse(false, null, "HTTP ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get series info", e)
            ApiResponse(false, null, e.message ?: "Unknown error")
        }
    }
}
```

---

## 📺 **HydraApiService - Live TV & VOD API**

### **API Endpoints**
```kotlin
// API Endpoints - Using the correct ones from working Hydra implementation
const val GET_LIVE_CATEGORIES = "get_live_categories"
const val GET_LIVE_STREAMS = "get_live_streams"
const val GET_VOD_CATEGORIES = "get_vod_categories"
const val GET_VOD_STREAMS = "get_vod_streams"
const val GET_SERIES_CATEGORIES = "get_series_categories"
const val GET_SERIES = "get_series"
const val GET_SERIES_INFO = "get_series_info"
const val GET_EPISODES = "get_episodes"
const val GET_EPISODES_INFO = "get_episodes_info"
const val GET_LIVE_FULL_EPG = "get_simple_data_table"
```

### **Date Format Handling**
```kotlin
// Date Formats for EPG and timestamps
private val EPG_FORMAT = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.getDefault())
private val STAMP_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
private val CLOCK_FORMAT = SimpleDateFormat("HH:mm", Locale.getDefault())
private val CLOCK12_FORMAT = SimpleDateFormat("hh:mm", Locale.getDefault())
private val CATCHUP_FORMAT = SimpleDateFormat("yyyy-MM-dd:HH-mm", Locale.getDefault())
```

---

## 📊 **Data Models**

### **1. API Response Models**
```kotlin
// Generic API Response
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String? = null
) {
    companion object {
        fun <T> Success(data: T): ApiResponse<T> = ApiResponse(true, data, null)
        fun Error(message: String): ApiResponse<Nothing> = ApiResponse(false, null, message)
    }
}

// Hydra API Response
data class HydraResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String? = null
) {
    companion object {
        fun <T> Success(data: T): HydraResponse<T> = HydraResponse(true, data, null)
        fun Error(message: String): HydraResponse<Nothing> = HydraResponse(false, null, message)
    }
}
```

### **2. Authentication Models**
```kotlin
// Login Response
data class LoginResponse(
    val user_info: UserInfo?,
    val server_info: ServerInfo?
)

// User Information
data class UserInfo(
    val username: String,
    val password: String,
    val status: String,
    val exp_date: String,
    val max_connections: String,
    val allowed_output_formats: List<String>
)

// Server Information
data class ServerInfo(
    val url: String,
    val port: String,
    val server_protocol: String
)
```

### **3. Series Content Models**
```kotlin
// Series Category
data class SeriesCategory(
    val category_id: String,
    val category_name: String,
    val parent_id: Int = 0
)

// Series Information
data class Series(
    val series_id: Int,
    val name: String,
    val cover: String,
    val plot: String,
    val cast: String,
    val director: String,
    val genre: String,
    val releaseDate: String,
    val rating: String,
    val rating_5based: Double,
    val category_id: String,
    val backdrop_path: List<String>? = null,
    val youtube_trailer: String = "",
    val episode_run_time: String = "0"
)

// Series Info with Episodes
data class SeriesInfo(
    val info: SeriesInfoDetails,
    val episodes: Map<String, List<Episode>>
)

// Series Details
data class SeriesInfoDetails(
    val name: String,
    val cover: String,
    val plot: String,
    val genre: String,
    val releaseDate: String,
    val rating: String,
    val rating_5based: Double,
    val backdrop_path: List<String>? = null,
    val youtube_trailer: String = ""
)

// Episode Information
data class Episode(
    val id: String,
    val title: String,
    val container_extension: String,
    val info: EpisodeInfo? = null
)

// Episode Details
data class EpisodeInfo(
    val rating: String,
    val releasedate: String
)
```

---

## 🔧 **API Integration & Usage**

### **1. Dependency Injection Setup**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideIPTVApiService(
        client: okhttp3.OkHttpClient,
        activityLogger: ActivityLogger
    ): IPTVApiService {
        return IPTVApiService(client, activityLogger)
    }

    @Provides
    @Singleton
    fun provideHydraApiService(
        @ApplicationContext context: Context,
        client: OkHttpClient,
        loggingService: ServerLoggingService,
        activityLogger: ActivityLogger,
        analyticsService: AnalyticsService
    ): HydraApiService {
        return HydraApiService(context, client, loggingService, activityLogger, analyticsService)
    }
}
```

### **2. ViewModel Integration**
```kotlin
@HiltViewModel
class SimpleSeriesViewModel @Inject constructor(
    private val iptvApiService: IPTVApiService,
    private val activityLogger: ActivityLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow<SeriesUiState>(SeriesUiState.Loading)
    val uiState: StateFlow<SeriesUiState> = _uiState.asStateFlow()

    // Load series categories
    fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = SeriesUiState.Loading
            
            activityLogger.logUserAction(
                className = "SimpleSeriesViewModel",
                methodName = "loadCategories",
                action = "Load Series Categories",
                lineNumber = 50
            )
            
            val result = iptvApiService.getSeriesCategories()
            
            if (result.success) {
                _uiState.value = SeriesUiState.Success(
                    categories = result.data ?: emptyList(),
                    series = emptyList()
                )
            } else {
                _uiState.value = SeriesUiState.Error(
                    message = result.message ?: "Failed to load categories"
                )
            }
        }
    }

    // Load series by category
    fun loadSeries(categoryId: String) {
        viewModelScope.launch {
            _uiState.value = SeriesUiState.Loading
            
            activityLogger.logUserAction(
                className = "SimpleSeriesViewModel",
                methodName = "loadSeries",
                action = "Load Series for Category: $categoryId",
                lineNumber = 80
            )
            
            val result = iptvApiService.getSeries(categoryId)
            
            if (result.success) {
                val currentState = _uiState.value
                if (currentState is SeriesUiState.Success) {
                    _uiState.value = currentState.copy(series = result.data ?: emptyList())
                }
            } else {
                _uiState.value = SeriesUiState.Error(
                    message = result.message ?: "Failed to load series"
                )
            }
        }
    }
}
```

### **3. API Service Configuration**
```kotlin
// Update credentials
fun updateCredentials(username: String, password: String, server: String = "hydraa.cc", port: String = "2095") {
    this.username = username
    this.password = password
    this.serverUrl = server
    this.serverPort = port
    this.isAuthenticated = false // Reset authentication
}

// Check authentication status
fun isAuthenticated(): Boolean = isAuthenticated

// Get authentication info
fun getAuthInfo(): Pair<String, String> {
    return Pair(username, password)
}
```

---

## 📱 **API Testing & Validation**

### **1. API Testing Functions**
```kotlin
// Test API connectivity
fun testApiConnectivity(): Boolean {
    return try {
        val url = buildApiUrl("")
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        response.isSuccessful
    } catch (e: Exception) {
        false
    }
}

// Test authentication
fun testAuthentication(): Boolean {
    return try {
        val result = runBlocking { authenticate() }
        result.success
    } catch (e: Exception) {
        false
    }
}

// Test series categories loading
fun testSeriesCategories(): Boolean {
    return try {
        val result = runBlocking { getSeriesCategories() }
        result.success && result.data?.isNotEmpty() == true
    } catch (e: Exception) {
        false
    }
}
```

### **2. Performance Testing**
```kotlin
// Measure API response time
fun measureApiResponseTime(action: String): Long {
    val startTime = System.currentTimeMillis()
    try {
        val url = buildApiUrl(action)
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val endTime = System.currentTimeMillis()
        return endTime - startTime
    } catch (e: Exception) {
        return -1
    }
}

// Test streaming URL generation
fun testStreamingUrlGeneration(episodeId: String): String {
    return buildSeriesStreamingUrl(episodeId, "mkv")
}
```

---

## 🚀 **Error Handling & Logging**

### **1. Activity Logging**
```kotlin
// Log API calls
activityLogger.logApiCall(
    className = "IPTVApiService",
    methodName = "authenticate",
    url = url,
    parameters = mapOf("username" to username),
    lineNumber = 150
)

// Log user actions
activityLogger.logUserAction(
    className = "SimpleSeriesViewModel",
    methodName = "loadCategories",
    action = "Load Series Categories",
    lineNumber = 50
)
```

### **2. Error Handling Patterns**
```kotlin
// Try-catch with logging
try {
    val response = client.newCall(request).execute()
    if (response.isSuccessful) {
        // Handle success
    } else {
        ApiResponse(false, null, "HTTP ${response.code}")
    }
} catch (e: Exception) {
    Log.e(TAG, "API call failed", e)
    activityLogger.logApiCall(
        className = "IPTVApiService",
        methodName = "methodName",
        url = url,
        error = e.message ?: "Unknown error",
        durationMs = 0,
        lineNumber = 200
    )
    ApiResponse(false, null, e.message ?: "Unknown error")
}
```

---

## 📊 **API Features Summary**

### **API Services**
| Service | Purpose | Endpoints |
|---------|---------|-----------|
| **IPTVService** | Basic IPTV operations | M3U playlist, EPG data |
| **EPGService** | Electronic Program Guide | EPG data fetching |
| **IPTVApiService** | Series content management | Categories, series, episodes |
| **HydraApiService** | Live TV & VOD content | Live streams, VOD, EPG |

### **Authentication & Security**
| Feature | Implementation | Security |
|---------|---------------|----------|
| **User Authentication** | Template-based URL building | Username/password in query params |
| **Session Management** | Authentication state tracking | Automatic re-authentication |
| **API Logging** | Activity logger integration | Full request/response tracking |
| **Error Handling** | Comprehensive try-catch blocks | Graceful error recovery |

### **Content Management**
| Content Type | API Endpoint | Data Model |
|--------------|--------------|------------|
| **Series Categories** | `get_series_categories` | `SeriesCategory` |
| **Series List** | `get_series` | `Series` |
| **Series Details** | `get_series_info` | `SeriesInfo` |
| **Episodes** | Embedded in series info | `Episode` |
| **Live Categories** | `get_live_categories` | `CategoryResponse` |
| **Live Streams** | `get_live_streams` | `LiveStream` |
| **VOD Content** | `get_vod_streams` | `VODStream` |

---

## 🎯 **API Benefits**

### **1. Content Discovery**
- **Series Categories:** Organized content browsing
- **Series Information:** Rich metadata and details
- **Episode Management:** Complete episode listings
- **Streaming URLs:** Direct content access

### **2. User Experience**
- **Fast Loading:** Efficient API responses
- **Error Recovery:** Graceful error handling
- **Activity Tracking:** User behavior monitoring
- **Performance Monitoring:** Response time tracking

### **3. Developer Experience**
- **Type Safety:** Strongly typed data models
- **Dependency Injection:** Clean architecture
- **Logging Integration:** Comprehensive debugging
- **Error Handling:** Robust error management

### **4. Scalability**
- **Modular Design:** Separate services for different content types
- **Template-based URLs:** Flexible endpoint configuration
- **Caching Support:** Efficient data management
- **Performance Optimization:** Response time monitoring

---

## 🚀 **Future Enhancements**

### **Planned Features**
1. **API Caching:** Implement response caching for better performance
2. **Rate Limiting:** Add rate limiting to prevent API abuse
3. **Retry Logic:** Implement automatic retry for failed requests
4. **Offline Support:** Cache data for offline viewing
5. **API Versioning:** Support for multiple API versions

### **Performance Improvements**
1. **Request Batching:** Batch multiple API requests
2. **Response Compression:** Enable gzip compression
3. **Connection Pooling:** Optimize HTTP connections
4. **Background Sync:** Periodic data synchronization
5. **Smart Caching:** Intelligent cache invalidation

---

## 📋 **Troubleshooting**

### **Common Issues**

#### **1. Authentication Failures**
```kotlin
// Check credentials
fun validateCredentials(username: String, password: String): Boolean {
    return username.isNotEmpty() && password.isNotEmpty()
}

// Handle authentication errors
fun handleAuthError(error: String): String {
    return when {
        error.contains("401") -> "Invalid credentials"
        error.contains("403") -> "Account expired"
        error.contains("429") -> "Too many requests"
        else -> "Authentication failed: $error"
    }
}
```

#### **2. Network Connectivity Issues**
```kotlin
// Check network connectivity
fun isNetworkAvailable(): Boolean {
    return try {
        val url = URL("https://www.google.com")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.requestMethod = "HEAD"
        val responseCode = connection.responseCode
        responseCode == 200
    } catch (e: Exception) {
        false
    }
}

// Handle network errors
fun handleNetworkError(exception: Exception): String {
    return when (exception) {
        is UnknownHostException -> "No internet connection"
        is SocketTimeoutException -> "Request timeout"
        is IOException -> "Network error"
        else -> "Unknown network error"
    }
}
```

#### **3. API Response Parsing Issues**
```kotlin
// Validate JSON response
fun validateJsonResponse(responseBody: String?): Boolean {
    return try {
        responseBody?.let { JSONObject(it) }
        true
    } catch (e: Exception) {
        false
    }
}

// Safe JSON parsing
fun safeJsonParse(jsonString: String, defaultValue: String = ""): String {
    return try {
        JSONObject(jsonString).getString("key")
    } catch (e: Exception) {
        defaultValue
    }
}
```

---

**Documentation Created:** December 2024  
**Last Updated:** December 2024  
**Status:** ✅ **COMPLETE & TESTED**  
**Version:** 1.0.0
