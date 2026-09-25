package pe.edu.ucsm.alertamesh

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import pe.edu.ucsm.alertamesh.model.ChatMessage
import pe.edu.ucsm.alertamesh.model.Profile
import pe.edu.ucsm.alertamesh.model.Validators
import pe.edu.ucsm.alertamesh.power.PowerMode
import java.util.UUID

private val NAVY = Color.parseColor("#0D2B45")
private val BLUE = Color.parseColor("#1976D2")
private val RED = Color.parseColor("#D32F2F")
private val BG = Color.parseColor("#EEF2F7")
private val FIELD = Color.parseColor("#EDF1F6")
private val DARK = Color.parseColor("#1B2733")
private val GREEN = Color.parseColor("#7CE0A0")
private val AMBER = Color.parseColor("#FFC857")
private val SOFT = Color.parseColor("#B8C7D9")

private const val REQ_PERMS = 100
private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

class MainActivity : Activity(), MeshNode.Listener {

    private lateinit var prefs: SharedPreferences
    private var node: MeshNode? = null
    private var profile: Profile? = null
    private var networkCode = ""

    private var messagesBox: LinearLayout? = null
    private var messagesScroll: ScrollView? = null
    private var statusView: TextView? = null
    private var peers = 0
    private var lowPower = false
    private var lastError: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("alertamesh", Context.MODE_PRIVATE)
        val name = prefs.getString("name", null)
        val dni = prefs.getString("dni", null)
        val code = prefs.getString("code", null)
        if (name != null && dni != null && code != null) {
            profile = Profile(name, dni, nodeId())
            networkCode = Validators.normalizeCode(code)
            showChat()
            ensurePermissionsAndStart()
        } else {
            showLogin()
        }
    }

    override fun onDestroy() {
        node?.stop()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- utilidades de UI

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun styledButton(label: String, color: Int) = Button(this).apply {
        text = label
        setAllCaps(false)
        textSize = 16f
        setTextColor(Color.WHITE)
        background = rounded(color, 12)
        setPadding(dp(16), dp(12), dp(16), dp(12))
    }

    private fun field(hintText: String, type: Int) = EditText(this).apply {
        hint = hintText
        inputType = type
        setSingleLine()
        setTextColor(DARK)
        setHintTextColor(Color.GRAY)
        background = rounded(FIELD, 12)
        setPadding(dp(14), dp(12), dp(14), dp(12))
    }

    private fun params(top: Int = 0) =
        LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(top) }

    private fun nodeId(): String =
        prefs.getString("nodeId", null)
            ?: UUID.randomUUID().toString().take(8).also {
                prefs.edit().putString("nodeId", it).apply()
            }

    // ---------------------------------------------------------------- pantalla de ingreso

    private fun showLogin() {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.WHITE, 20)
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        val etName = field("Nombre completo (solo letras)",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS).apply {
            filters = arrayOf(LettersOnlyFilter(), InputFilter.LengthFilter(60))
        }
        val etDni = field("DNI (8 dígitos)", InputType.TYPE_CLASS_NUMBER).apply {
            filters = arrayOf(InputFilter.LengthFilter(8))
        }
        val etCode = field("Código de la red",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS).apply {
            setText("alertamesh")
        }
        val hint = TextView(this).apply {
            text = "Todos los teléfonos de tu grupo deben usar el mismo código de red " +
                "(no distingue mayúsculas). Los mensajes viajan cifrados con él."
            textSize = 12f
            setTextColor(Color.GRAY)
        }
        val error = TextView(this).apply {
            setTextColor(RED)
            textSize = 13f
        }
        val enter = styledButton("Ingresar", BLUE)

        card.addView(etName, params())
        card.addView(etDni, params(12))
        card.addView(etCode, params(12))
        card.addView(hint, params(6))
        card.addView(error, params(6))
        card.addView(enter, params(14))

        enter.setOnClickListener {
            val name = Validators.normalizeName(etName.text.toString())
            val dni = etDni.text.toString().trim()
            val code = Validators.normalizeCode(etCode.text.toString())
            when {
                !Validators.isValidName(name) ->
                    error.text = "El nombre solo puede tener letras y espacios (mínimo 3 letras)."
                !Regex("\\d{8}").matches(dni) -> error.text = "El DNI debe tener 8 dígitos."
                code.length < 4 -> error.text = "El código de red debe tener al menos 4 caracteres."
                else -> {
                    prefs.edit().putString("name", name).putString("dni", dni)
                        .putString("code", code).apply()
                    profile = Profile(name, dni, nodeId())
                    networkCode = code
                    showChat()
                    ensurePermissionsAndStart()
                }
            }
        }

        val title = TextView(this).apply {
            text = "AlertaMesh"
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "Mensajería de emergencia sin internet"
            textSize = 15f
            setTextColor(SOFT)
            gravity = Gravity.CENTER
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
            addView(title, params())
            addView(subtitle, params(4))
            addView(card, params(28))
        }
        setContentView(ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(NAVY)
            addView(root)
        })
    }

    // ---------------------------------------------------------------- pantalla de chat

    private fun showChat() {
        val p = profile ?: return

        val title = TextView(this).apply {
            text = "AlertaMesh"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        val exit = TextView(this).apply {
            text = "Salir"
            textSize = 14f
            setTextColor(SOFT)
            setPadding(dp(12), dp(6), 0, dp(6))
            setOnClickListener { logout() }
        }
        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(title, LinearLayout.LayoutParams(0, WRAP, 1f))
            addView(exit)
        }
        val who = TextView(this).apply {
            text = "${p.name} · DNI ${p.dni}"
            textSize = 13f
            setTextColor(SOFT)
        }
        val status = TextView(this).apply {
            textSize = 13f
            setTextColor(AMBER)
        }
        statusView = status
        val powerSwitch = Switch(this).apply {
            text = "Modo bajo consumo"
            setTextColor(Color.WHITE)
            isChecked = lowPower
            setOnCheckedChangeListener { _, checked ->
                lowPower = checked
                node?.powerMode = if (checked) PowerMode.LOW_POWER else PowerMode.NORMAL
                updateStatus()
            }
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(NAVY)
            setPadding(dp(16), dp(14), dp(16), dp(12))
            addView(titleRow, params())
            addView(who, params(2))
            addView(status, params(6))
            addView(powerSwitch, params(6))
        }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        val scroll = ScrollView(this).apply { addView(box) }
        messagesBox = box
        messagesScroll = scroll

        val input = EditText(this).apply {
            hint = "Escribe tu mensaje…"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE
            maxLines = 4
            filters = arrayOf(InputFilter.LengthFilter(500))
            setTextColor(DARK)
            setHintTextColor(Color.GRAY)
            background = rounded(FIELD, 12)
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        val sosButton = styledButton("SOS", RED).apply {
            setOnClickListener { sendFrom(input, true) }
        }
        val sendButton = styledButton("Enviar", BLUE).apply {
            setOnClickListener { sendFrom(input, false) }
        }
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(sosButton, LinearLayout.LayoutParams(0, WRAP, 1f).apply { marginEnd = dp(8) })
            addView(sendButton, LinearLayout.LayoutParams(0, WRAP, 2f))
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            addView(input, params())
            addView(buttons, params(8))
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
            addView(header, params())
            addView(scroll, LinearLayout.LayoutParams(MATCH, 0, 1f))
            addView(bar, params())
        }
        setContentView(root)
        updateStatus()
    }

    private fun sendFrom(input: EditText, sos: Boolean) {
        var text = input.text.toString().trim()
        if (text.isEmpty()) {
            if (!sos) return
            text = "¡Necesito ayuda!"
        }
        val n = node
        if (n == null) {
            lastError = "La red aún no inició. Revisa los permisos."
            updateStatus()
            return
        }
        n.sendText(text, sos)
        input.text.clear()
    }

    private fun addBubble(m: ChatMessage) {
        val box = messagesBox ?: return
        val (bg, fg) = when {
            m.sos -> Pair(RED, Color.WHITE)
            m.mine -> Pair(BLUE, Color.WHITE)
            else -> Pair(Color.WHITE, DARK)
        }
        val who = if (m.mine) "Tú" else "${m.senderName} · DNI ${m.senderDni} · ${m.hops} salto(s)"
        val bubble = TextView(this).apply {
            text = (if (m.sos) "🆘 SOS\n" else "") + who + "\n" + m.text
            textSize = 15f
            setTextColor(fg)
            background = rounded(bg, 14)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            maxWidth = (resources.displayMetrics.widthPixels * 0.8).toInt()
        }
        val lp = LinearLayout.LayoutParams(WRAP, WRAP).apply {
            topMargin = dp(8)
            gravity = if (m.mine) Gravity.END else Gravity.START
        }
        box.addView(bubble, lp)
        messagesScroll?.post { messagesScroll?.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun updateStatus() {
        val tv = statusView ?: return
        val mode = if (lowPower) "bajo consumo" else "normal"
        tv.setTextColor(if (peers > 0) GREEN else AMBER)
        val base = if (peers > 0) "● $peers vecino(s) conectado(s)" else "● Buscando vecinos…"
        tv.text = base + " · modo " + mode + (lastError?.let { "\n⚠ $it" } ?: "")
    }

    private fun logout() {
        node?.stop()
        node = null
        profile = null
        peers = 0
        lastError = null
        prefs.edit().remove("name").remove("dni").remove("code").apply()
        showLogin()
    }

    // ---------------------------------------------------------------- permisos y arranque

    private fun requiredPermissions(): List<String> {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 31) {
            list += Manifest.permission.BLUETOOTH_ADVERTISE
            list += Manifest.permission.BLUETOOTH_CONNECT
            list += Manifest.permission.BLUETOOTH_SCAN
        }
        if (Build.VERSION.SDK_INT >= 33) {
            list += Manifest.permission.NEARBY_WIFI_DEVICES
        }
        // Nearby Connections necesita ubicación (COARSE + FINE juntos, se piden en la misma solicitud).
        list += Manifest.permission.ACCESS_COARSE_LOCATION
        list += Manifest.permission.ACCESS_FINE_LOCATION
        return list
    }

    private fun ensurePermissionsAndStart() {
        val missing = requiredPermissions().filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) startNode() else requestPermissions(missing.toTypedArray(), REQ_PERMS)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQ_PERMS) return
        if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startNode()
        } else {
            lastError = "Faltan permisos. Actívalos en Ajustes → Aplicaciones → AlertaMesh " +
                "(Ubicación: \"Precisa\" y Dispositivos cercanos)."
            updateStatus()
        }
    }

    private fun startNode() {
        val p = profile ?: return
        if (node != null) return
        node = MeshNode(applicationContext, p, networkCode, this).also {
            it.powerMode = if (lowPower) PowerMode.LOW_POWER else PowerMode.NORMAL
            it.start()
        }
    }

    // ---------------------------------------------------------------- MeshNode.Listener

    override fun onChat(msg: ChatMessage) = addBubble(msg)

    override fun onPeers(count: Int) {
        peers = count
        if (count > 0) lastError = null
        updateStatus()
    }

    override fun onStatus(text: String) {
        lastError = text
        updateStatus()
    }
}

/** Bloquea al escribir todo lo que no sea letra (incluye tildes y ñ) o espacio. */
private class LettersOnlyFilter : InputFilter {
    override fun filter(
        source: CharSequence, start: Int, end: Int,
        dest: android.text.Spanned, dstart: Int, dend: Int
    ): CharSequence? {
        val sb = StringBuilder()
        for (i in start until end) {
            val c = source[i]
            if (c.isLetter() || c == ' ') sb.append(c)
        }
        return if (sb.length == end - start) null else sb.toString()
    }
}
