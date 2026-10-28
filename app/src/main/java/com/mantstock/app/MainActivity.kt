package com.mantstock.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mantstock.app.data.SupabaseManager
import com.mantstock.app.ui.theme.MantStockTheme
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MantStockTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PantallaPrincipal(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Serializable
data class UsuarioPerfil(
    @SerialName("id_usuario") val idUsuario: String,
    @SerialName("primer_nombre") val primerNombre: String,
    @SerialName("primer_apellido") val primerApellido: String,
    @SerialName("correo") val correo: String,
    @SerialName("rol") val rol: String,
    @SerialName("estado") val estado: String
)

@Serializable
data class Almacen(
    @SerialName("id_almacen") val id: Long,
    val nombre: String,
    val descripcion: String? = null,
    val estado: String = "Activo"
)

@Serializable
data class AlmacenInsert(
    val nombre: String,
    val descripcion: String? = null,
    val estado: String = "Activo"
)

@Serializable
data class Categoria(
    @SerialName("id_categoria") val id: Long,
    val nombre: String,
    val descripcion: String? = null
)

@Serializable
data class CategoriaInsert(
    val nombre: String,
    val descripcion: String? = null
)

// La columna imagen se eliminó deliberadamente del modelo.
@Serializable
data class Articulo(
    @SerialName("id_item") val id: Long,
    val nombre: String,
    val descripcion: String,
    @SerialName("numero_serie") val numeroSerie: String? = null,
    val precio: Double,
    val cantidad: Int,
    val ubicacion: String? = null,
    @SerialName("id_categoria") val idCategoria: Long,
    @SerialName("id_almacen") val idAlmacen: Long,
    @SerialName("tipo_gestion") val tipoGestion: String,
    val estado: String = "Activo"
)

@Serializable
data class ArticuloGuardar(
    val nombre: String,
    val descripcion: String,
    @SerialName("numero_serie") val numeroSerie: String? = null,
    val precio: Double = 0.0,
    val cantidad: Int = 0,
    val ubicacion: String? = null,
    @SerialName("id_categoria") val idCategoria: Long,
    @SerialName("id_almacen") val idAlmacen: Long,
    @SerialName("tipo_gestion") val tipoGestion: String,
    val estado: String = "Activo"
)

@Serializable
data class Movimiento(
    @SerialName("id_movimiento") val id: Long,
    val tipo: String,
    val cantidad: Int,
    @SerialName("fecha_movimiento") val fecha: String,
    @SerialName("descripcion_salida") val descripcion: String,
    val observaciones: String = "",
    @SerialName("id_item") val idItem: Long,
    @SerialName("id_almacen") val idAlmacen: Long,
    @SerialName("id_usuario") val idUsuario: String
)

@Serializable
data class UsuarioLista(
    @SerialName("id_usuario") val id: String,
    @SerialName("primer_nombre") val nombre: String,
    @SerialName("primer_apellido") val apellido: String,
    val correo: String,
    val rol: String,
    val estado: String
)

@Serializable
data class SolicitudCrearUsuario(
    @SerialName("primer_nombre") val primerNombre: String,
    @SerialName("primer_apellido") val primerApellido: String
)

@Serializable
data class RespuestaCrearUsuario(
    val ok: Boolean = true,
    @SerialName("id_usuario") val idUsuario: String? = null,
    val correo: String,
    val contrasena: String
)

@Composable
fun PantallaPrincipal(modifier: Modifier = Modifier) {
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf("") }
    var perfil by remember { mutableStateOf<UsuarioPerfil?>(null) }
    val alcance = rememberCoroutineScope()

    if (perfil != null) {
        PantallaInicio(
            perfil = perfil!!,
            onCerrarSesion = {
                alcance.launch {
                    try {
                        SupabaseManager.client.auth.signOut()
                        perfil = null
                        correo = ""
                        contrasena = ""
                        mensajeError = ""
                    } catch (e: Exception) {
                        mensajeError = "No se pudo cerrar la sesión: ${e.message}"
                    }
                }
            },
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("MantStock", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text("Sistema de inventario de mantenimiento")
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it; mensajeError = "" },
            label = { Text("Correo electrónico") },
            singleLine = true,
            enabled = !cargando,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = contrasena,
            onValueChange = { contrasena = it; mensajeError = "" },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !cargando,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                if (correo.isBlank() || contrasena.isBlank()) {
                    mensajeError = "Escribe tu correo y contraseña."
                    return@Button
                }
                alcance.launch {
                    cargando = true
                    mensajeError = ""
                    try {
                        val cliente = SupabaseManager.client
                        cliente.auth.signInWith(Email) {
                            email = correo.trim()
                            password = contrasena
                        }
                        val usuarioAuth = cliente.auth.currentUserOrNull()
                            ?: throw Exception("No se pudo obtener la sesión del usuario.")
                        val usuario = cliente.from("usuarios").select {
                            filter { eq("id_usuario", usuarioAuth.id) }
                        }.decodeList<UsuarioPerfil>().firstOrNull()
                            ?: throw Exception("La cuenta inició sesión, pero no tiene perfil en la tabla usuarios.")
                        if (!usuario.estado.equals("Activo", true)) {
                            cliente.auth.signOut()
                            throw Exception("Tu cuenta está inactiva. Contacta al administrador.")
                        }
                        val rolValido = usuario.rol.equals("Administrador", true) ||
                                usuario.rol.equals("Usuario", true) ||
                                usuario.rol.equals("Usuario Normal", true)
                        if (!rolValido) {
                            cliente.auth.signOut()
                            throw Exception("El rol '${usuario.rol}' no es válido.")
                        }
                        perfil = usuario
                    } catch (e: Exception) {
                        mensajeError = when {
                            e.message.orEmpty().contains("Invalid login credentials", true) ->
                                "Correo o contraseña incorrectos."
                            else -> "No se pudo iniciar sesión: ${e.message ?: "error desconocido"}"
                        }
                    } finally {
                        cargando = false
                    }
                }
            },
            enabled = !cargando,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (cargando) CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
            else Text("Iniciar sesión")
        }
        if (mensajeError.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(mensajeError, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun PantallaInicio(
    perfil: UsuarioPerfil,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val admin = perfil.rol.equals("Administrador", ignoreCase = true)
    var seccion by remember { mutableStateOf(if (admin) "Inicio" else "Artículos") }
    var seccionArticulo by remember { mutableStateOf("Registrar") }
    var recarga by remember { mutableIntStateOf(0) }
    var resetFormulario by remember { mutableIntStateOf(0) }
    var cargando by remember { mutableStateOf(false) }
    var buscandoActualizar by remember { mutableStateOf(false) }
    var buscandoBaja by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var exito by remember { mutableStateOf("") }
    var almacenes by remember { mutableStateOf(listOf<Almacen>()) }
    var categorias by remember { mutableStateOf(listOf<Categoria>()) }
    var articulos by remember { mutableStateOf(listOf<Articulo>()) }
    var movimientos by remember { mutableStateOf(listOf<Movimiento>()) }
    var usuarios by remember { mutableStateOf(listOf<UsuarioLista>()) }
    var credencialesUsuarioCreado by remember { mutableStateOf<RespuestaCrearUsuario?>(null) }
    var usuarioParaCambiarEstado by remember { mutableStateOf<UsuarioLista?>(null) }
    var estadoDestinoUsuario by remember { mutableStateOf("Inactivo") }
    var mostrarCambioEstadoUsuario by remember { mutableStateOf(false) }
    var busquedaActualizar by remember { mutableStateOf("") }
    var resultadosActualizar by remember { mutableStateOf(listOf<Articulo>()) }
    var seleccionadoActualizar by remember { mutableStateOf<Articulo?>(null) }
    var busquedaBaja by remember { mutableStateOf("") }
    var resultadosBaja by remember { mutableStateOf(listOf<Articulo>()) }
    var seleccionadoBaja by remember { mutableStateOf<Articulo?>(null) }
    var mostrarCancelarDialogo by remember { mutableStateOf(false) }
    var mostrarBajaDialogo by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(perfil.idUsuario, seccion, recarga) {
        cargando = true
        error = ""
        try {
            val db = SupabaseManager.client
            when (seccion) {
                "Almacenes" -> almacenes = db.from("almacenes").select().decodeList<Almacen>()
                "Categorías" -> categorias = db.from("categorias").select().decodeList<Categoria>()
                "Artículos" -> {
                    almacenes = db.from("almacenes").select().decodeList<Almacen>()
                    categorias = db.from("categorias").select().decodeList<Categoria>()
                    articulos = db.from("items").select().decodeList<Articulo>()
                }
                "Movimientos" -> movimientos = db.from("movimientos").select().decodeList<Movimiento>()
                "Usuarios" -> if (admin) usuarios = db.from("usuarios").select().decodeList<UsuarioLista>()
                "Inicio" -> {
                    almacenes = db.from("almacenes").select().decodeList<Almacen>()
                    categorias = db.from("categorias").select().decodeList<Categoria>()
                    articulos = db.from("items").select().decodeList<Articulo>()
                }
            }
        } catch (e: Exception) {
            error = "No se pudieron cargar los datos: ${e.message}"
        } finally {
            cargando = false
        }
    }

    // Solo consulta al tener al menos 3 caracteres; 350 ms de pausa evitan peticiones por cada tecla inmediata.
    LaunchedEffect(seccion, seccionArticulo, busquedaActualizar, recarga) {
        if (seccion != "Artículos" || seccionArticulo != "Actualizar" || busquedaActualizar.trim().length < 3) {
            resultadosActualizar = emptyList()
            buscandoActualizar = false
        } else {
            delay(350)
            buscandoActualizar = true
            try {
                resultadosActualizar = SupabaseManager.client.from("items").select {
                    filter {
                        ilike("nombre", "%${busquedaActualizar.trim()}%")
                        eq("estado", "Activo")
                    }
                    limit(count = 10)
                }.decodeList<Articulo>()
            } catch (e: Exception) {
                error = "Error al buscar artículos: ${e.message}"
                resultadosActualizar = emptyList()
            } finally {
                buscandoActualizar = false
            }
        }
    }

    LaunchedEffect(seccion, seccionArticulo, busquedaBaja, recarga) {
        if (seccion != "Artículos" || seccionArticulo != "Baja" || busquedaBaja.trim().length < 3) {
            resultadosBaja = emptyList()
            buscandoBaja = false
        } else {
            delay(350)
            buscandoBaja = true
            try {
                resultadosBaja = SupabaseManager.client.from("items").select {
                    filter {
                        ilike("nombre", "%${busquedaBaja.trim()}%")
                        eq("estado", "Activo")
                    }
                    limit(count = 10)
                }.decodeList<Articulo>()
            } catch (e: Exception) {
                error = "Error al buscar artículos: ${e.message}"
                resultadosBaja = emptyList()
            } finally {
                buscandoBaja = false
            }
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("MantStock", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(if (admin) "Panel de administración" else "Panel de usuario")
                }
                OutlinedButton(onClick = onCerrarSesion) { Text("Cerrar sesión") }
            }
            Spacer(Modifier.height(10.dp))
            Text("Hola, ${perfil.primerNombre} ${perfil.primerApellido}", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (admin) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                NavButton("Inicio", seccion, { seccion = it }, Modifier.weight(1f))
                                NavButton("Almacenes", seccion, { seccion = it }, Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                NavButton("Categorías", seccion, { seccion = it }, Modifier.weight(1f))
                                NavButton("Artículos", seccion, { seccion = it }, Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                NavButton("Movimientos", seccion, { seccion = it }, Modifier.weight(1f))
                                NavButton("Usuarios", seccion, { seccion = it }, Modifier.weight(1f))
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                NavButton("Artículos", seccion, { seccion = it }, Modifier.weight(1f))
                                NavButton("Movimientos", seccion, { seccion = it }, Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Divider()
                    Spacer(Modifier.height(8.dp))
                }

                if (cargando) item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
                }
                if (error.isNotBlank()) item { Text(error, color = MaterialTheme.colorScheme.error) }
                if (exito.isNotBlank()) item { Text(exito, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }

                when (seccion) {
                    "Inicio" -> {
                        item { Text("Resumen del inventario", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                        item { ResumenCard("Almacenes registrados", almacenes.size.toString()) }
                        item { ResumenCard("Categorías registradas", categorias.size.toString()) }
                        item { ResumenCard("Artículos registrados", articulos.size.toString()) }
                        item { Text("Selecciona una sección para registrar o consultar información.") }
                        // No se muestra el botón de actualizar datos en Inicio.
                    }

                    "Almacenes" -> {
                        item {
                            key(resetFormulario) {
                                FormularioAlmacen { nombre, descripcion ->
                                    scope.launch {
                                        try {
                                            error = ""; exito = ""; cargando = true
                                            SupabaseManager.client.from("almacenes").insert(
                                                AlmacenInsert(nombre, descripcion.ifBlank { null })
                                            )
                                            exito = "Almacén registrado correctamente."
                                            resetFormulario++
                                            recarga++
                                        } catch (e: Exception) { error = "No se pudo registrar el almacén: ${e.message}" }
                                        finally { cargando = false }
                                    }
                                }
                            }
                        }
                        item { Text("Almacenes registrados (${almacenes.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                        items(almacenes, key = { it.id }) { a ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(a.nombre, fontWeight = FontWeight.Bold)
                                    Text("Estado: ${a.estado}")
                                    if (!a.descripcion.isNullOrBlank()) Text(a.descripcion)
                                }
                            }
                        }
                    }

                    "Categorías" -> {
                        item {
                            key(resetFormulario) {
                                FormularioCategoria { nombre, descripcion ->
                                    scope.launch {
                                        try {
                                            error = ""; exito = ""; cargando = true
                                            SupabaseManager.client.from("categorias").insert(
                                                CategoriaInsert(nombre, descripcion.ifBlank { null })
                                            )
                                            exito = "Categoría registrada correctamente."
                                            resetFormulario++
                                            recarga++
                                        } catch (e: Exception) { error = "No se pudo registrar la categoría: ${e.message}" }
                                        finally { cargando = false }
                                    }
                                }
                            }
                        }
                        item { Text("Categorías registradas (${categorias.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                        items(categorias, key = { it.id }) { c ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(c.nombre, fontWeight = FontWeight.Bold)
                                    if (!c.descripcion.isNullOrBlank()) Text(c.descripcion)
                                }
                            }
                        }
                    }

                    "Artículos" -> {
                        item {
                            Text("Gestión de artículos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf("Registrar", "Actualizar", "Baja").forEach { tab ->
                                    NavButton(tab, seccionArticulo, {
                                        seccionArticulo = it
                                        exito = ""
                                        error = ""
                                        if (it != "Actualizar") { seleccionadoActualizar = null; busquedaActualizar = "" }
                                        if (it != "Baja") { seleccionadoBaja = null; busquedaBaja = "" }
                                    }, Modifier.weight(1f))
                                }
                            }
                        }

                        if (seccionArticulo == "Registrar") {
                            item {
                                key(resetFormulario) {
                                    FormularioArticulo(
                                        articuloInicial = null,
                                        categorias = categorias,
                                        almacenes = almacenes,
                                        textoBoton = "Guardar artículo",
                                        resetToken = resetFormulario,
                                        onGuardar = { articulo ->
                                            scope.launch {
                                                try {
                                                    error = ""; exito = ""; cargando = true
                                                    SupabaseManager.client.from("items").insert(articulo)
                                                    exito = "Artículo registrado correctamente."
                                                    resetFormulario++
                                                    recarga++
                                                } catch (e: Exception) { error = "No se pudo registrar el artículo: ${e.message}" }
                                                finally { cargando = false }
                                            }
                                        }
                                    )
                                }
                            }
                            item { Text("Artículos registrados (${articulos.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                            items(articulos.take(30), key = { it.id }) { a -> TarjetaArticulo(a) }
                        }

                        if (seccionArticulo == "Actualizar") {
                            item {
                                OutlinedTextField(
                                    value = busquedaActualizar,
                                    onValueChange = { busquedaActualizar = it; seleccionadoActualizar = null; exito = "" },
                                    label = { Text("Buscar artículo por nombre") },
                                    supportingText = { Text("Escribe al menos 3 caracteres. Se muestran máximo 10 coincidencias.") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (buscandoActualizar) item { Text("Buscando...") }
                            if (busquedaActualizar.trim().length >= 3 && !buscandoActualizar && resultadosActualizar.isEmpty()) {
                                item { Text("No se encontraron artículos activos con ese nombre.") }
                            }
                            items(resultadosActualizar, key = { it.id }) { a ->
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(a.nombre, fontWeight = FontWeight.Bold)
                                        Text("Cantidad: ${a.cantidad} · Precio: ${a.precio}")
                                        Text("Categoría: ${categorias.find { it.id == a.idCategoria }?.nombre ?: "No encontrada"}")
                                        Text("Almacén: ${almacenes.find { it.id == a.idAlmacen }?.nombre ?: "No encontrado"}")
                                        OutlinedButton(onClick = { seleccionadoActualizar = a }, modifier = Modifier.fillMaxWidth()) {
                                            Text("Editar este artículo")
                                        }
                                    }
                                }
                            }
                            if (seleccionadoActualizar != null) {
                                item {
                                    Text("Editar: ${seleccionadoActualizar!!.nombre}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    key(resetFormulario, seleccionadoActualizar!!.id) {
                                        FormularioArticulo(
                                            articuloInicial = seleccionadoActualizar,
                                            categorias = categorias,
                                            almacenes = almacenes,
                                            textoBoton = "Actualizar artículo",
                                            resetToken = resetFormulario,
                                            onGuardar = { articulo ->
                                                val idEditar = seleccionadoActualizar!!.id
                                                scope.launch {
                                                    try {
                                                        error = ""; exito = ""; cargando = true
                                                        SupabaseManager.client.from("items").update(articulo) {
                                                            filter { eq("id_item", idEditar) }
                                                        }
                                                        exito = "Artículo actualizado correctamente."
                                                        seleccionadoActualizar = null
                                                        busquedaActualizar = ""
                                                        resetFormulario++
                                                        recarga++
                                                    } catch (e: Exception) { error = "No se pudo actualizar el artículo: ${e.message}" }
                                                    finally { cargando = false }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (seccionArticulo == "Baja") {
                            item {
                                OutlinedTextField(
                                    value = busquedaBaja,
                                    onValueChange = { busquedaBaja = it; seleccionadoBaja = null; exito = "" },
                                    label = { Text("Buscar artículo para dar de baja") },
                                    supportingText = { Text("Escribe al menos 3 caracteres. Solo artículos activos; máximo 10 resultados.") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (buscandoBaja) item { Text("Buscando...") }
                            if (busquedaBaja.trim().length >= 3 && !buscandoBaja && resultadosBaja.isEmpty()) {
                                item { Text("No se encontraron artículos activos con ese nombre.") }
                            }
                            items(resultadosBaja, key = { it.id }) { a ->
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(a.nombre, fontWeight = FontWeight.Bold)
                                        Text("ID interno: ${a.id} · Cantidad: ${a.cantidad} · Estado: ${a.estado}")
                                        OutlinedButton(onClick = { seleccionadoBaja = a }, modifier = Modifier.fillMaxWidth()) {
                                            Text("Seleccionar para baja")
                                        }
                                    }
                                }
                            }
                            if (seleccionadoBaja != null) {
                                item {
                                    Card(Modifier.fillMaxWidth()) {
                                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Confirmar baja lógica", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text("Artículo: ${seleccionadoBaja!!.nombre}")
                                            Text("Cantidad actual: ${seleccionadoBaja!!.cantidad}")
                                            Text("El artículo no se eliminará de la base de datos; su estado cambiará a Inactivo.")
                                            Button(onClick = { mostrarBajaDialogo = true }, modifier = Modifier.fillMaxWidth()) {
                                                Text("Dar de baja")
                                            }
                                            OutlinedButton(onClick = { mostrarCancelarDialogo = true }, modifier = Modifier.fillMaxWidth()) {
                                                Text("Cancelar")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "Movimientos" -> {
                        item { Text("Historial de movimientos (${movimientos.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                        items(movimientos, key = { it.id }) { m ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("${m.tipo} · Cantidad: ${m.cantidad}", fontWeight = FontWeight.Bold)
                                    Text("Movimiento #${m.id} · Artículo ID: ${m.idItem} · Almacén ID: ${m.idAlmacen}")
                                    Text("Fecha: ${m.fecha}")
                                    Text(m.descripcion)
                                    if (m.observaciones.isNotBlank()) Text("Observaciones: ${m.observaciones}")
                                }
                            }
                        }
                    }

                    "Usuarios" -> if (admin) {
                        item {
                            key(resetFormulario) {
                                FormularioUsuario(
                                    resetToken = resetFormulario,
                                    onCrear = { solicitud ->
                                        scope.launch {
                                            try {
                                                error = ""
                                                exito = ""
                                                credencialesUsuarioCreado = null
                                                cargando = true
                                                val respuesta = SupabaseManager.client.functions.invoke(
                                                    function = "crear-usuario",
                                                    body = solicitud
                                                ).body<RespuestaCrearUsuario>()
                                                credencialesUsuarioCreado = respuesta
                                                exito = "Usuario creado correctamente. Guarda las credenciales mostradas."
                                                resetFormulario++
                                                recarga++
                                            } catch (e: Exception) {
                                                error = "No se pudo crear el usuario. Verifica functions-kt, install(Functions) y el despliegue de crear-usuario: ${e.message}"
                                            } finally {
                                                cargando = false
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        if (credencialesUsuarioCreado != null) {
                            item {
                                val credenciales = credencialesUsuarioCreado!!
                                Card(Modifier.fillMaxWidth()) {
                                    Column(
                                        Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "Cuenta creada: guarda estas credenciales",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text("Correo", fontWeight = FontWeight.SemiBold)
                                        Text(credenciales.correo)
                                        Text("Contraseña inicial", fontWeight = FontWeight.SemiBold)
                                        Text(credenciales.contrasena)
                                        Text(
                                            "La contraseña se genera a partir del nombre y apellido. Es predecible; úsala solo para la demostración académica y cámbiala antes de un uso real.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        OutlinedButton(
                                            onClick = { credencialesUsuarioCreado = null },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Ocultar credenciales")
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                "Usuarios registrados (${usuarios.count { !it.rol.equals("Administrador", true) }})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(
                            usuarios.filter { !it.rol.equals("Administrador", ignoreCase = true) },
                            key = { it.id }
                        ) { u ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(
                                    Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        "${u.nombre} ${u.apellido}",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(u.correo)
                                    Text("Rol: Usuario Normal")
                                    Text("Estado: ${u.estado}")
                                    if (u.estado.equals("Activo", ignoreCase = true)) {
                                        Button(
                                            onClick = {
                                                usuarioParaCambiarEstado = u
                                                estadoDestinoUsuario = "Inactivo"
                                                mostrarCambioEstadoUsuario = true
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Dar de baja (lógica)")
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {
                                                usuarioParaCambiarEstado = u
                                                estadoDestinoUsuario = "Activo"
                                                mostrarCambioEstadoUsuario = true
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Reactivar usuario")
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }

            if (seccion != "Inicio") {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { recarga++; exito = "" },
                        modifier = Modifier.weight(1f)
                    ) { Text("Actualizar datos") }
                    OutlinedButton(
                        onClick = { mostrarCancelarDialogo = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancelar") }
                }
            }
        }
    }

    if (mostrarCancelarDialogo) {
        AlertDialog(
            onDismissRequest = { mostrarCancelarDialogo = false },
            title = { Text("Cancelar operación") },
            text = { Text("¿Deseas cancelar? Se limpiarán los campos y la búsqueda actual. No se guardarán los cambios que aún no hayas confirmado.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarCancelarDialogo = false
                    resetFormulario++
                    seleccionadoActualizar = null
                    seleccionadoBaja = null
                    busquedaActualizar = ""
                    busquedaBaja = ""
                    resultadosActualizar = emptyList()
                    resultadosBaja = emptyList()
                    error = ""
                    exito = ""
                    seccionArticulo = "Registrar"
                }) { Text("Sí, cancelar") }
            },
            dismissButton = { TextButton(onClick = { mostrarCancelarDialogo = false }) { Text("Continuar editando") } }
        )
    }

    if (mostrarBajaDialogo && seleccionadoBaja != null) {
        AlertDialog(
            onDismissRequest = { mostrarBajaDialogo = false },
            title = { Text("Confirmar baja lógica") },
            text = { Text("¿Dar de baja el artículo '${seleccionadoBaja!!.nombre}'? El registro se conservará y su estado cambiará a Inactivo.") },
            confirmButton = {
                TextButton(onClick = {
                    val id = seleccionadoBaja!!.id
                    mostrarBajaDialogo = false
                    scope.launch {
                        try {
                            error = ""; exito = ""; cargando = true
                            SupabaseManager.client.from("items").update({ set("estado", "Inactivo") }) {
                                filter { eq("id_item", id) }
                            }
                            exito = "Artículo dado de baja correctamente."
                            seleccionadoBaja = null
                            busquedaBaja = ""
                            resetFormulario++
                            recarga++
                        } catch (e: Exception) { error = "No se pudo dar de baja el artículo: ${e.message}" }
                        finally { cargando = false }
                    }
                }) { Text("Confirmar baja") }
            },
            dismissButton = { TextButton(onClick = { mostrarBajaDialogo = false }) { Text("No, volver") } }
        )
    }


    if (mostrarCambioEstadoUsuario && usuarioParaCambiarEstado != null) {
        val usuarioObjetivo = usuarioParaCambiarEstado!!
        val accion = if (estadoDestinoUsuario == "Inactivo") "dar de baja" else "reactivar"
        AlertDialog(
            onDismissRequest = { mostrarCambioEstadoUsuario = false },
            title = { Text("Confirmar ${if (estadoDestinoUsuario == "Inactivo") "baja" else "reactivación"} lógica") },
            text = {
                Text("¿Deseas $accion a ${usuarioObjetivo.nombre} ${usuarioObjetivo.apellido}? No se borrará su registro; solo cambiará su estado.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarCambioEstadoUsuario = false
                        scope.launch {
                            try {
                                error = ""
                                exito = ""
                                cargando = true
                                SupabaseManager.client.from("usuarios").update({
                                    set("estado", estadoDestinoUsuario)
                                }) {
                                    filter { eq("id_usuario", usuarioObjetivo.id) }
                                }
                                usuarios = usuarios.map { usuario ->
                                    if (usuario.id == usuarioObjetivo.id) {
                                        usuario.copy(estado = estadoDestinoUsuario)
                                    } else usuario
                                }
                                exito = if (estadoDestinoUsuario == "Inactivo") {
                                    "Usuario dado de baja lógicamente."
                                } else {
                                    "Usuario reactivado correctamente."
                                }
                                usuarioParaCambiarEstado = null
                                recarga++
                            } catch (e: Exception) {
                                error = "No se pudo cambiar el estado del usuario: ${e.message}"
                            } finally {
                                cargando = false
                            }
                        }
                    }
                ) { Text(if (estadoDestinoUsuario == "Inactivo") "Confirmar baja" else "Confirmar reactivación") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCambioEstadoUsuario = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun NavButton(texto: String, actual: String, cambiar: (String) -> Unit, modifier: Modifier = Modifier) {
    if (texto == actual) Button(onClick = { cambiar(texto) }, modifier = modifier) { Text(texto) }
    else OutlinedButton(onClick = { cambiar(texto) }, modifier = modifier) { Text(texto) }
}

@Composable
private fun ResumenCard(titulo: String, valor: String) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(titulo, fontWeight = FontWeight.Medium)
            Text(valor, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun TarjetaArticulo(a: Articulo) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(a.nombre, fontWeight = FontWeight.Bold)
            Text("Estado: ${a.estado} · Gestión: ${a.tipoGestion}")
            Text("Cantidad: ${a.cantidad} · Precio: ${a.precio}")
            Text("Categoría: ${a.idCategoria} · Almacén: ${a.idAlmacen}")
            Text(a.descripcion)
            if (!a.numeroSerie.isNullOrBlank()) Text("Serie: ${a.numeroSerie}")
            if (!a.ubicacion.isNullOrBlank()) Text("Ubicación: ${a.ubicacion}")
        }
    }
}

@Composable
private fun FormularioAlmacen(alGuardar: (String, String) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Registrar almacén", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre del almacén *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(descripcion, { descripcion = it }, label = { Text("Descripción (opcional)") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { if (nombre.isNotBlank()) alGuardar(nombre.trim(), descripcion.trim()) }, enabled = nombre.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text("Guardar almacén")
        }
    }
}

@Composable
private fun FormularioCategoria(alGuardar: (String, String) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Registrar categoría", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre de la categoría *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(descripcion, { descripcion = it }, label = { Text("Descripción (opcional)") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { if (nombre.isNotBlank()) alGuardar(nombre.trim(), descripcion.trim()) }, enabled = nombre.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text("Guardar categoría")
        }
    }
}

@Composable
private fun FormularioArticulo(
    articuloInicial: Articulo?,
    categorias: List<Categoria>,
    almacenes: List<Almacen>,
    textoBoton: String,
    resetToken: Int,
    onGuardar: (ArticuloGuardar) -> Unit
) {
    var nombre by remember(articuloInicial?.id, resetToken) { mutableStateOf(articuloInicial?.nombre ?: "") }
    var descripcion by remember(articuloInicial?.id, resetToken) { mutableStateOf(articuloInicial?.descripcion ?: "") }
    var serie by remember(articuloInicial?.id, resetToken) { mutableStateOf(articuloInicial?.numeroSerie ?: "") }
    var precio by remember(articuloInicial?.id, resetToken) { mutableStateOf(articuloInicial?.precio?.toString() ?: "0") }
    var cantidad by remember(articuloInicial?.id, resetToken) { mutableStateOf(articuloInicial?.cantidad?.toString() ?: "0") }
    var ubicacion by remember(articuloInicial?.id, resetToken) { mutableStateOf(articuloInicial?.ubicacion ?: "") }
    var categoriaId by remember(articuloInicial?.id, resetToken) { mutableStateOf<Long?>(articuloInicial?.idCategoria) }
    var almacenId by remember(articuloInicial?.id, resetToken) { mutableStateOf<Long?>(articuloInicial?.idAlmacen) }
    var tipo by remember(articuloInicial?.id, resetToken) { mutableStateOf(articuloInicial?.tipoGestion ?: "Unidad") }

    val opcionesCategorias = categorias.map { it.id to it.nombre }
    val opcionesAlmacenes = almacenes.map { it.id to it.nombre }
    val precioNumero = precio.replace(',', '.').toDoubleOrNull()
    val cantidadNumero = cantidad.toIntOrNull()
    val valido = nombre.isNotBlank() && descripcion.isNotBlank() && categoriaId != null && almacenId != null &&
            precioNumero != null && precioNumero >= 0.0 && cantidadNumero != null && cantidadNumero >= 0 &&
            categorias.any { it.id == categoriaId } && almacenes.any { it.id == almacenId }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (articuloInicial == null) "Registrar artículo" else "Actualizar artículo seleccionado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(descripcion, { descripcion = it }, label = { Text("Descripción *") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(serie, { serie = it }, label = { Text("Número de serie (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(precio, { precio = it }, label = { Text("Precio") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(cantidad, { cantidad = it }, label = { Text("Cantidad") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(ubicacion, { ubicacion = it }, label = { Text("Ubicación (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        SelectorBuscable("Categoría *", opcionesCategorias, categoriaId, { categoriaId = it })
        SelectorBuscable("Almacén *", opcionesAlmacenes, almacenId, { almacenId = it })
        Text("Tipo de gestión")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Unidad", "Serie", "Retornable").forEach { opcion ->
                if (tipo == opcion) Button(onClick = { tipo = opcion }, modifier = Modifier.weight(1f)) { Text(opcion) }
                else OutlinedButton(onClick = { tipo = opcion }, modifier = Modifier.weight(1f)) { Text(opcion) }
            }
        }
        if (categorias.isEmpty() || almacenes.isEmpty()) {
            Text("Primero registra al menos una categoría y un almacén.", color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = {
                if (valido) {
                    onGuardar(
                        ArticuloGuardar(
                            nombre = nombre.trim(),
                            descripcion = descripcion.trim(),
                            numeroSerie = serie.trim().ifBlank { null },
                            precio = precioNumero!!,
                            cantidad = cantidadNumero!!,
                            ubicacion = ubicacion.trim().ifBlank { null },
                            idCategoria = categoriaId!!,
                            idAlmacen = almacenId!!,
                            tipoGestion = tipo,
                            estado = "Activo"
                        )
                    )
                }
            },
            enabled = valido,
            modifier = Modifier.fillMaxWidth()
        ) { Text(textoBoton) }
    }
}

@Composable
private fun SelectorBuscable(
    etiqueta: String,
    opciones: List<Pair<Long, String>>,
    seleccionadoId: Long?,
    alSeleccionar: (Long?) -> Unit
) {
    var texto by remember { mutableStateOf(opciones.firstOrNull { it.first == seleccionadoId }?.second.orEmpty()) }
    var desplegado by remember { mutableStateOf(false) }
    LaunchedEffect(seleccionadoId, opciones) {
        if (seleccionadoId != null) texto = opciones.firstOrNull { it.first == seleccionadoId }?.second.orEmpty()
    }
    val filtradas = if (texto.isBlank()) opciones else opciones.filter { it.second.contains(texto, ignoreCase = true) }

    Column {
        Box {
            OutlinedTextField(
                value = texto,
                onValueChange = { nuevo ->
                    texto = nuevo
                    desplegado = true
                    if (seleccionadoId != null) alSeleccionar(null)
                },
                label = { Text(etiqueta) },
                placeholder = { Text("Escribe para filtrar nombres") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().onFocusChanged { desplegado = it.isFocused }
            )
            DropdownMenu(
                expanded = desplegado && filtradas.isNotEmpty(),
                onDismissRequest = { desplegado = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                filtradas.take(30).forEach { opcion ->
                    DropdownMenuItem(
                        text = { Text(opcion.second) },
                        onClick = {
                            texto = opcion.second
                            alSeleccionar(opcion.first)
                            desplegado = false
                        }
                    )
                }
            }
        }
        if (desplegado && texto.isNotBlank() && filtradas.isEmpty()) {
            Text("No hay coincidencias con ese nombre.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun normalizarNombreUsuario(texto: String): String {
    val sinAcentos = java.text.Normalizer.normalize(
        texto.trim().lowercase(),
        java.text.Normalizer.Form.NFD
    ).replace(Regex("\\p{M}+"), "")
    return sinAcentos.filter { it in 'a'..'z' || it in '0'..'9' }
}

@Composable
private fun FormularioUsuario(
    resetToken: Int,
    onCrear: (SolicitudCrearUsuario) -> Unit
) {
    var nombre by remember(resetToken) { mutableStateOf("") }
    var apellido by remember(resetToken) { mutableStateOf("") }

    val nombreGenerado = normalizarNombreUsuario(nombre)
    val apellidoGenerado = normalizarNombreUsuario(apellido)
    val correoGenerado = if (nombreGenerado.isNotBlank() && apellidoGenerado.isNotBlank()) {
        "$nombreGenerado.$apellidoGenerado@mantstock.mx"
    } else {
        ""
    }
    val contrasenaGenerada = nombreGenerado + apellidoGenerado
    val valido = nombreGenerado.isNotBlank() && apellidoGenerado.isNotBlank() &&
            contrasenaGenerada.length >= 6 && correoGenerado.isNotBlank()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Alta de usuario normal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text("Captura el nombre y primer apellido. El correo y la contraseña inicial se generan automáticamente.")

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = apellido,
            onValueChange = { apellido = it },
            label = { Text("Primer apellido *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = correoGenerado,
            onValueChange = {},
            readOnly = true,
            label = { Text("Correo generado automáticamente") },
            placeholder = { Text("nombre.apellido@mantstock.mx") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (contrasenaGenerada.isNotBlank() && contrasenaGenerada.length < 6) {
            Text(
                "La combinación de nombre y apellido debe tener al menos 6 caracteres para que Supabase acepte la contraseña.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = {
                if (valido) {
                    onCrear(
                        SolicitudCrearUsuario(
                            primerNombre = nombre.trim(),
                            primerApellido = apellido.trim()
                        )
                    )
                }
            },
            enabled = valido,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Crear usuario normal")
        }
    }
}
