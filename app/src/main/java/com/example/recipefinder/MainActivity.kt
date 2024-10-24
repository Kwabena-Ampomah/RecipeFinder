package com.example.recipefinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import coil.compose.rememberImagePainter
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Retrofit API Interface Slide 48 Retrofit API
interface SpoonacularApi {
    @GET("recipes/complexSearch")
    suspend fun searchRecipes(
        @Query("query") query: String?,
        @Query("cuisine") cuisine: String? = null,
        @Query("diet") diet: String? = null,
        @Query("maxCalories") maxCalories: Int? = null,
        @Query("apiKey") apiKey: String
    ): RecipeResponse
}

//Moshi Parsing Slide 56
@JsonClass(generateAdapter = true)
data class RecipeResponse(
    @Json(name = "results") val recipes: List<Recipe>
)

@JsonClass(generateAdapter = true)
data class Recipe(
    val title: String,
    val image: String,
    val id: Int
)
//(Slide 18: Threads and thread
class MainActivity : ComponentActivity() {

  //Slide 13: ViewModels gather data
    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> get() = _recipes

    companion object {
        private const val API_KEY = "43b1bebabd104a019286cd8917867a2a"
//Slide 50: Retrofit
        val api: SpoonacularApi = Retrofit.Builder()
            .baseUrl("https://api.spoonacular.com/")
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }).build()
            )
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(SpoonacularApi::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RecipeApp()
        }
    }

    private fun fetchRecipes(query: String?) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = api.searchRecipes(query, apiKey = API_KEY)
                _recipes.value = response.recipes
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Composable
    fun RecipeApp() {
        val recipeList by recipes.collectAsState()
        var query by remember { mutableStateOf("") }

        Column(modifier = Modifier.padding(16.dp)) {
            TextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search Recipes") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { fetchRecipes(query) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Search")
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(recipeList) { recipe ->
                    RecipeItem(recipe)
                }
            }
        }
    }

    @Composable
    fun RecipeItem(recipe: Recipe) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable {  }
        ) {
            Image(
                painter = rememberImagePainter(data = recipe.image),
                contentDescription = "Recipe Image",
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(recipe.title, modifier = Modifier.weight(1f))
        }
    }
}
