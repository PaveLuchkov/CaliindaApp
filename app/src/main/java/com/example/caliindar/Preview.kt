import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Vertices
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import kotlin.math.max
import kotlin.random.Random
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.translate

// --- Основной Composable экрана ---
// @Preview(showBackground = true, device = "id:pixel_6a")
@Composable
fun CalendarTimelineScreen() {
    Scaffold(
        topBar = { CalendarAppBar() },
        bottomBar = { CalendarBottomBar() },
        backgroundColor = MaterialTheme.colors.surface// Фон всего экрана
    ) { paddingValues ->
        // Используем Box для возможности наложения фона (если нужно сложнее)
        // и основного контента
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
            // .background( /* Сюда можно добавить фоновый рисунок, если нужно */)
        ) {

            BackgroundShapes(colorScheme)

            // Основной контент - таймлайн и события
            TimelineContent()
        }
    }
}

// --- Верхняя панель приложения ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarAppBar() {
    CenterAlignedTopAppBar(
        title = { Text("Caliinda", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        navigationIcon = {
            IconButton(onClick = {}) {
                Icon(Icons.Filled.Today, contentDescription = "Calendar View")
            }
        },
        actions = {
            IconButton(onClick = { /* Действие для настроек */ }) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        },
    )
}

// --- Нижняя панель навигации/действий ---
@Composable
fun CalendarBottomBar() {
    BottomAppBar(
        backgroundColor = colorScheme.surface,
        cutoutShape = CircleShape // Можно добавить вырез для FAB если нужно
    ) {
        IconButton(onClick = { /* TODO: Действие клавиатуры */ }) {
            Icon(Icons.Filled.Keyboard, contentDescription = "Keyboard Input")
        }
        Spacer(Modifier.weight(1f)) // Занимает все доступное пространство
        FloatingActionButton(
            onClick = {},
            modifier = Modifier
                .size(56.dp))
        {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "Voice Input")
        }


    }
}


@Composable
fun BackgroundShapes(colorScheme: ColorScheme) {
    Box(
        modifier = Modifier
            .fillMaxSize()
        // .padding(16.dp)
    ) {
        // Создаем фигуру звезды один раз
        val starShape = remember {
            RoundedPolygon.star(
                17,
                rounding = CornerRounding(0.95f),
        //        radius = 30f,
            )
        }
        val clipStar = remember(starShape) {
            RoundedPolygonShape(polygon = starShape)
        }
        val star2Shape = remember {
            RoundedPolygon.star(
                4,
                rounding = CornerRounding(0.6f),
                //        radius = 30f,
            )
        }
        val clip2Star = remember(starShape) {
            RoundedPolygonShape(polygon = star2Shape)
        }
        val starContainerSize = 300.dp
        val star2ContainerSize = 200.dp
        // Холст для отрисовки
        Box(
            modifier = Modifier
                .size(starContainerSize)
                .align(Alignment.TopEnd)
                .offset(
                    x = starContainerSize * 0.2f,
                    y = -starContainerSize * 0.1f
                )
                .clip(clipStar)
                .background(colorScheme.surfaceContainer),
        ) {

        }
        Box(
            modifier = Modifier
                .size(star2ContainerSize)
                .align(Alignment.CenterStart)
                .offset(
                    x = -star2ContainerSize * 0.4f
                )
                .clip(clip2Star)
                .background(colorScheme.surfaceContainer),
        ) {

        }
        Box(
            modifier = Modifier
                .padding(start = 42.dp)
                .fillMaxHeight()
                .width(3.dp)
                .background(colorScheme.scrim)

        )
    }
}

// --- Контент с таймлайном и событиями ---
@Composable
fun TimelineContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp) // Отступы по бокам
    ) {
        // Можно добавить невидимую линию слева с помощью Box или Canvas,
        // но для простого превью пока опустим.

        Spacer(modifier = Modifier.height(1.dp)) // Отступ сверху до первого события

        // Каждое событие - это строка с временем и карточкой события
        TimelineEventRow("8:00", "9:30", "Docs sorting") // Используем Close как заглушку
        TimelineEventRow("10:00", "11:30", "Meet-up") // Используем Brightness5 как заглушку
        TimelineEventRow("11:00", "16:00", "Working", true) // Широкий блок
        TimelineEventRow("17:00", "19:00", "Swimming") // Используем ThumbUp как заглушку
        TimelineEventRow("20:00", "21:00", "Dinner with family") // Используем Circle как заглушку
        TimelineEventRow("20:30", "21:00", "Discuss furniture") // Используем Circle как заглушку

        Spacer(modifier = Modifier.weight(1f)) // Занимает оставшееся место внизу
    }
}

// --- Компонент для строки события (время + карточка) ---
@Composable
fun TimelineEventRow(
    startTime: String,
    endTime: String,
    title: String,
    isCurrent: Boolean = false, // Флаг для текущих
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.Top // Выравниваем по верху времени
    ) {

        // Карточка события
        EventCard(
            title = title,
            isCurrent = isCurrent,
            startTime = startTime,
            endTime = endTime
        )
    }
}

// --- Компонент для карточки события ---
@Composable
fun EventCard(
    title: String,
    isCurrent: Boolean = false, // Флаг для текущего события
    startTime: String,
    endTime: String
) {
    val polygon = remember {
        createRandomShape()
    }
    val clip = remember(polygon) {
        RoundedPolygonShape(polygon = polygon)
    }
    Card(
        modifier = Modifier
            .then(if (isCurrent) {Modifier.height(120.dp)} else {Modifier.height(70.dp)}) // TODO: c 70.dp и ниже текст начинает ужиматься и неправильно форматироваться
            .fillMaxWidth()
            .padding(end = 80.dp),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = if (isCurrent) {colorScheme.tertiaryContainer} else {
            colorScheme.primary},
        elevation = if (isCurrent) {10.dp} else {0.dp}
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 0.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(
              verticalArrangement = Arrangement.SpaceBetween,
              horizontalAlignment = Alignment.Start,
              modifier = Modifier
                  .fillMaxHeight()
          )
             {
              Text(text = startTime, fontSize = 10.sp, color = (if (isCurrent) {colorScheme.onTertiaryContainer} else {
                  colorScheme.onPrimary}))
                 Box(
                     modifier = Modifier
                         .align(Alignment.CenterHorizontally)
                         .size(16.dp) // Размер иконки
                         .clip(clip) // Сделаем круглым фоном для простоты
                         .background(if (isCurrent) {colorScheme.onTertiaryContainer} else {
                             colorScheme.onPrimary}) // Цвет фона иконки
                 )
                 Text(text = endTime, fontSize = 10.sp, color = (if (isCurrent) {colorScheme.onTertiaryContainer} else {
                     colorScheme.onPrimary}))
            }

            Spacer(modifier = Modifier.width(10.dp)) // Отступ между иконкой и текстом

            Text(
                text = title,
                color = (if (isCurrent) {colorScheme.onTertiaryContainer} else {
                    colorScheme.onPrimary}), // Цвет фона иконки,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium // Средняя жирность
            )
        }
    }
}

fun RoundedPolygon.getBounds() = calculateBounds().let { Rect(it[0], it[1], it[2], it[3]) }
class RoundedPolygonShape(
    private val polygon: RoundedPolygon,
    private var matrix: Matrix = Matrix()
) : Shape {
    private var path = Path()
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        path.rewind()
        path = polygon.toPath().asComposePath()
        matrix.reset()
        val bounds = polygon.getBounds()
        val maxDimension = max(bounds.width, bounds.height)
        matrix.scale(size.width / maxDimension, size.height / maxDimension)
        matrix.translate(-bounds.left, -bounds.top)

        path.transform(matrix)
        return Outline.Generic(path)
    }
}

fun createRandomShape(
    isStar: Boolean = Random.nextBoolean(),
    vertices: Int = Random.nextInt(3, 5),
    rounding: CornerRounding = CornerRounding(Random.nextFloat() * 0.9f),
    radius: Float = Random.nextFloat() * 10f
): RoundedPolygon {
    return RoundedPolygon(numVertices = vertices, radius = radius, rounding = rounding)
}



// --- Превью ---
@Preview(showBackground = true, device = "id:pixel_4") // Пример устройства
@Composable
fun DefaultPreview() {
    MaterialTheme { // Обертка в тему для стилей Material Design
        CalendarTimelineScreen()
    }
}

// --- Активность (если нужно запустить на устройстве/эмуляторе) ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CalendarTimelineScreen()
            }
        }
    }
}