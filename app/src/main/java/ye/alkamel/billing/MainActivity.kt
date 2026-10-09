package ye.alkamel.billing

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("alkamel_local_db", MODE_PRIVATE) }
    private val bg = Color.rgb(246, 248, 251)
    private val ink = Color.rgb(31, 43, 58)
    private val muted = Color.rgb(112, 124, 139)
    private val green = Color.rgb(19, 105, 82)
    private val paleGreen = Color.rgb(226, 243, 236)
    private val orange = Color.rgb(222, 133, 42)
    private lateinit var root: LinearLayout
    private lateinit var body: LinearLayout
    private lateinit var nav: LinearLayout
    private var page = "الرئيسية"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = green
        window.navigationBarColor = Color.rgb(24, 30, 37)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        setContentView(root)
        render()
    }

    private fun render() {
        root.removeAllViews()
        root.addView(header(), lp(-1, -2))
        val scroll = ScrollView(this).apply { fillViewport = true }
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(24))
        }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        when (page) {
            "الرئيسية" -> dashboard()
            "المشتركون" -> subscribersPage()
            "الفواتير" -> invoicesPage()
            "التقارير" -> reportsPage()
        }
        root.addView(bottomNav(), lp(-1, dp(66)))
    }

    private fun header(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            setBackgroundColor(green)
        }
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(text("الكامل", 23f, Color.WHITE, true))
        titles.addView(text("للفواتير والتحصيل", 12f, Color.rgb(215, 237, 229)))
        bar.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))
        val settings = TextView(this).apply {
            text = "⚙"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setOnClickListener { settingsDialog() }
        }
        bar.addView(settings, lp(dp(42), dp(42)))
        return bar
    }

    private fun dashboard() {
        body.addView(text("أهلًا بك 👋", 23f, ink, true))
        body.addView(text("ملخص نشاطك المالي اليوم", 14f, muted), lp(-1, -2, 0, 4, 0, 16))
        val invoices = read("invoices")
        val customers = read("customers")
        var total = 0.0; var paid = 0.0
        for (i in 0 until invoices.length()) {
            val x = invoices.optJSONObject(i) ?: continue
            val amount = x.optDouble("amount")
            total += amount
            if (x.optBoolean("paid")) paid += amount
        }
        val outstanding = total - paid
        val stats = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row1.addView(statCard("إجمالي الفواتير", money(total), "▤", green), LinearLayout.LayoutParams(0, dp(112), 1f))
        row1.addView(spaceW(10))
        row1.addView(statCard("المبالغ المحصلة", money(paid), "✓", Color.rgb(40, 139, 102)), LinearLayout.LayoutParams(0, dp(112), 1f))
        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row2.addView(statCard("المتبقي للتحصيل", money(outstanding), "◷", orange), LinearLayout.LayoutParams(0, dp(112), 1f))
        row2.addView(spaceW(10))
        row2.addView(statCard("عدد المشتركين", customers.length().toString(), "♙", Color.rgb(75, 105, 171)), LinearLayout.LayoutParams(0, dp(112), 1f))
        stats.addView(row1, lp(-1, -2))
        stats.addView(spaceH(10))
        stats.addView(row2, lp(-1, -2))
        body.addView(stats, lp(-1, -2))
        body.addView(sectionTitle("إجراءات سريعة"), lp(-1, -2, 0, 22, 0, 10))
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(actionButton("＋ فاتورة جديدة", green) { showAddInvoice() }, LinearLayout.LayoutParams(0, dp(52), 1f))
        actions.addView(spaceW(10))
        actions.addView(actionButton("＋ مشترك جديد", Color.rgb(65, 87, 124)) { showAddCustomer() }, LinearLayout.LayoutParams(0, dp(52), 1f))
        body.addView(actions, lp(-1, -2))
        body.addView(sectionTitle("أحدث الفواتير", "عرض الكل") { page = "الفواتير"; render() }, lp(-1, -2, 0, 24, 0, 8))
        if (invoices.length() == 0) {
            body.addView(emptyState("لا توجد فواتير حتى الآن", "ابدأ بإضافة أول فاتورة للمشتركين."), lp(-1, -2))
        } else {
            var shown = 0
            for (i in invoices.length() - 1 downTo 0) {
                if (shown++ >= 4) break
                body.addView(invoiceCard(invoices.optJSONObject(i) ?: continue), lp(-1, -2, 0, 0, 0, 9))
            }
        }
    }

    private fun subscribersPage() {
        body.addView(sectionTitle("إدارة المشتركين"), lp(-1, -2, 0, 2, 0, 12))
        body.addView(actionButton("＋ إضافة مشترك جديد", green) { showAddCustomer() }, lp(-1, dp(50), 0, 0, 0, 14))
        val customers = read("customers")
        if (customers.length() == 0) body.addView(emptyState("قائمة المشتركين فارغة", "أضف بيانات المشتركين لتسهيل إصدار الفواتير لهم."), lp(-1, -2))
        for (i in customers.length() - 1 downTo 0) {
            val c = customers.optJSONObject(i) ?: continue
            val card = card()
            card.addView(text(c.optString("name"), 17f, ink, true))
            val phone = c.optString("phone")
            if (phone.isNotBlank()) card.addView(text("الهاتف: $phone", 13f, muted), lp(-1, -2, 0, 5, 0, 0))
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            row.addView(text("إصدار فاتورة لهذا المشترك", 13f, green), LinearLayout.LayoutParams(0, dp(40), 1f))
            row.addView(smallButton("فاتورة") { showAddInvoice(c.optString("name")) })
            card.addView(row, lp(-1, -2, 0, 6, 0, 0))
            body.addView(card, lp(-1, -2, 0, 0, 0, 10))
        }
    }

    private fun invoicesPage() {
        body.addView(sectionTitle("الفواتير"), lp(-1, -2, 0, 2, 0, 12))
        body.addView(actionButton("＋ إنشاء فاتورة", green) { showAddInvoice() }, lp(-1, dp(50), 0, 0, 0, 14))
        val invoices = read("invoices")
        if (invoices.length() == 0) body.addView(emptyState("لا توجد فواتير", "ستظهر الفواتير هنا مرتبة من الأحدث إلى الأقدم."), lp(-1, -2))
        for (i in invoices.length() - 1 downTo 0) {
            body.addView(invoiceCard(invoices.optJSONObject(i) ?: continue), lp(-1, -2, 0, 0, 0, 10))
        }
    }

    private fun reportsPage() {
        body.addView(sectionTitle("التقارير المالية"), lp(-1, -2, 0, 2, 0, 12))
        val invoices = read("invoices")
        var total = 0.0; var paid = 0.0; var countPaid = 0; var countDue = 0
        for (i in 0 until invoices.length()) {
            val x = invoices.optJSONObject(i) ?: continue
            total += x.optDouble("amount")
            if (x.optBoolean("paid")) { paid += x.optDouble("amount"); countPaid++ } else countDue++
        }
        val card = card()
        card.addView(text("ملخص الفواتير", 18f, ink, true))
        reportLine(card, "عدد الفواتير", invoices.length().toString())
        reportLine(card, "الفواتير المسددة", countPaid.toString())
        reportLine(card, "الفواتير غير المسددة", countDue.toString())
        reportLine(card, "الإجمالي", money(total))
        reportLine(card, "المحصّل", money(paid))
        reportLine(card, "المتبقي", money(total - paid))
        body.addView(card, lp(-1, -2))
        body.addView(text("تُحسب التقارير من البيانات المحفوظة على هذا الجهاز.", 12f, muted), lp(-1, -2, 0, 12, 0, 0))
    }

    private fun invoiceCard(item: JSONObject): View {
        val c = card()
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        info.addView(text(item.optString("name"), 17f, ink, true))
        val date = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()).format(Date(item.optLong("createdAt")))
        info.addView(text(date, 12f, muted), lp(-1, -2, 0, 4, 0, 0))
        row.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        val paid = item.optBoolean("paid")
        row.addView(pill(if (paid) "مسددة" else "غير مسددة", if (paid) paleGreen else Color.rgb(255, 241, 220), if (paid) green else Color.rgb(161, 93, 20)))
        c.addView(row, lp(-1, -2))
        c.addView(text(money(item.optDouble("amount")) + " ريال", 20f, green, true), lp(-1, -2, 0, 12, 0, 0))
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        if (!paid) buttons.addView(smallButton("تسجيل التحصيل") { markPaid(item.optLong("id")) })
        buttons.addView(spaceW(8))
        buttons.addView(smallButton("حذف") { confirmDelete(item.optLong("id")) })
        c.addView(buttons, lp(-1, -2, 0, 10, 0, 0))
        return c
    }

    private fun showAddCustomer() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(8), dp(22), 0) }
        val name = EditText(this).apply { hint = "اسم المشترك"; setSingleLine(true) }
        val phone = EditText(this).apply { hint = "رقم الهاتف (اختياري)"; inputType = android.text.InputType.TYPE_CLASS_PHONE; setSingleLine(true) }
        box.addView(name); box.addView(phone)
        AlertDialog.Builder(this).setTitle("إضافة مشترك").setView(box)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حفظ", null).create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val n = name.text.toString().trim()
                        if (n.isEmpty()) { name.error = "أدخل اسم المشترك"; return@setOnClickListener }
                        val arr = read("customers")
                        arr.put(JSONObject().put("id", System.currentTimeMillis()).put("name", n).put("phone", phone.text.toString().trim()))
                        save("customers", arr)
                        dialog.dismiss()
                        render()
                    }
                }
                dialog.show()
            }
    }

    private fun showAddInvoice(prefill: String = "") {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(8), dp(22), 0) }
        val name = EditText(this).apply { hint = "اسم المشترك"; setSingleLine(true); setText(prefill) }
        val amount = EditText(this).apply {
            hint = "قيمة الفاتورة بالريال"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setSingleLine(true)
        }
        box.addView(name); box.addView(amount)
        AlertDialog.Builder(this).setTitle("إنشاء فاتورة جديدة").setView(box)
            .setNegativeButton("إلغاء", null).setPositiveButton("حفظ", null).create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val n = name.text.toString().trim()
                        val a = amount.text.toString().trim().toDoubleOrNull()
                        if (n.isEmpty()) { name.error = "أدخل اسم المشترك"; return@setOnClickListener }
                        if (a == null || a <= 0) { amount.error = "أدخل مبلغًا صحيحًا"; return@setOnClickListener }
                        val arr = read("invoices")
                        arr.put(JSONObject().put("id", System.currentTimeMillis()).put("name", n)
                            .put("amount", a).put("paid", false).put("createdAt", System.currentTimeMillis()))
                        save("invoices", arr)
                        dialog.dismiss()
                        render()
                    }
                }
                dialog.show()
            }
    }

    private fun markPaid(id: Long) {
        val arr = read("invoices")
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            if (item.optLong("id") == id) { item.put("paid", true); item.put("paidAt", System.currentTimeMillis()); break }
        }
        save("invoices", arr)
        render()
        Toast.makeText(this, "تم تسجيل التحصيل", Toast.LENGTH_SHORT).show()
    }

    private fun confirmDelete(id: Long) {
        AlertDialog.Builder(this).setTitle("حذف الفاتورة")
            .setMessage("هل أنت متأكد من حذف هذه الفاتورة؟")
            .setNegativeButton("إلغاء", null).setPositiveButton("حذف") { _, _ ->
                val old = read("invoices"); val next = JSONArray()
                for (i in 0 until old.length()) {
                    val x = old.optJSONObject(i) ?: continue
                    if (x.optLong("id") != id) next.put(x)
                }
                save("invoices", next); render()
            }.show()
    }

    private fun settingsDialog() {
        val options = arrayOf("معلومات التطبيق", "النسخ الاحتياطي")
        AlertDialog.Builder(this).setTitle("الإعدادات").setItems(options) { _, which ->
            if (which == 0) AlertDialog.Builder(this).setTitle("الكامل للفواتير والتحصيل")
                .setMessage("تطبيق محلي لإدارة المشتركين والفواتير والتحصيل. البيانات الحالية محفوظة على الجهاز.")
                .setPositiveButton("حسنًا", null).show()
            else AlertDialog.Builder(this).setTitle("النسخ الاحتياطي")
                .setMessage("ميزة تصدير النسخ الاحتياطية واستعادتها ستُضاف ضمن استكمال وظائف التطبيق. لا تعتبر هذه الشاشة بديلًا عن النسخة الاحتياطية.")
                .setPositiveButton("حسنًا", null).show()
        }.show()
    }

    private fun bottomNav(): View {
        nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            elevation = dp(8).toFloat()
        }
        val items = listOf("الرئيسية" to "⌂", "المشتركون" to "♙", "الفواتير" to "▤", "التقارير" to "▥")
        for ((label, icon) in items) {
            val active = page == label
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, dp(7), 0, dp(5))
                setOnClickListener { page = label; render() }
            }
            item.addView(TextView(this).apply {
                text = icon; textSize = 21f; gravity = Gravity.CENTER
                setTextColor(if (active) green else muted)
            })
            item.addView(TextView(this).apply {
                text = label; textSize = 11f; gravity = Gravity.CENTER
                setTypeface(null, if (active) Typeface.BOLD else Typeface.NORMAL)
                setTextColor(if (active) green else muted)
            })
            nav.addView(item, LinearLayout.LayoutParams(0, -1, 1f))
        }
        return nav
    }

    private fun statCard(title: String, value: String, symbol: String, accent: Int): View {
        val c = card().apply { setPadding(dp(12), dp(12), dp(12), dp(10)) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val icon = TextView(this).apply {
            text = symbol; textSize = 17f; gravity = Gravity.CENTER; setTextColor(accent)
            background = shape(if (accent == orange) Color.rgb(255, 241, 220) else paleGreen, 12)
        }
        top.addView(icon, lp(dp(34), dp(34)))
        top.addView(spaceW(7))
        top.addView(text(title, 11f, muted), LinearLayout.LayoutParams(0, -2, 1f))
        c.addView(top)
        c.addView(text(value, 17f, ink, true), lp(-1, -2, 0, 12, 0, 0))
        return c
    }

    private fun sectionTitle(title: String, action: String? = null, onAction: (() -> Unit)? = null): View {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(text(title, 18f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
        if (action != null && onAction != null) row.addView(TextView(this).apply {
            text = action; textSize = 13f; setTextColor(green); setPadding(dp(8), dp(8), 0, dp(8)); setOnClickListener { onAction() }
        })
        return row
    }

    private fun emptyState(title: String, subtitle: String): View {
        val c = card().apply { gravity = Gravity.CENTER; setPadding(dp(18), dp(26), dp(18), dp(26)) }
        c.addView(TextView(this).apply { text = "▤"; textSize = 32f; setTextColor(green); gravity = Gravity.CENTER })
        c.addView(TextView(this).apply { text = title; textSize = 16f; setTextColor(ink); setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER; setPadding(0, dp(8), 0, dp(5)) })
        c.addView(TextView(this).apply { text = subtitle; textSize = 13f; setTextColor(muted); gravity = Gravity.CENTER })
        return c
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(15), dp(14), dp(15), dp(14))
        background = shape(Color.WHITE, 18)
        elevation = dp(2).toFloat()
    }

    private fun pill(label: String, color: Int, fg: Int): View = TextView(this).apply {
        text = label; textSize = 11f; setTextColor(fg); gravity = Gravity.CENTER
        setPadding(dp(10), dp(6), dp(10), dp(6)); background = shape(color, 20)
    }

    private fun actionButton(label: String, color: Int, action: () -> Unit): View = TextView(this).apply {
        text = label; textSize = 14f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        setTypeface(null, Typeface.BOLD); background = shape(color, 14); elevation = dp(1).toFloat()
        setOnClickListener { action() }
    }

    private fun smallButton(label: String, action: () -> Unit): View = TextView(this).apply {
        text = label; textSize = 12f; setTextColor(green); gravity = Gravity.CENTER
        setPadding(dp(12), dp(9), dp(12), dp(9)); background = shape(paleGreen, 10)
        setOnClickListener { action() }
    }

    private fun reportLine(parent: LinearLayout, label: String, value: String) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(11), 0, dp(11)) }
        row.addView(text(label, 14f, muted), LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(text(value, 15f, ink, true))
        parent.addView(row, lp(-1, -2))
        val line = View(this).apply { setBackgroundColor(Color.rgb(235, 238, 242)) }
        parent.addView(line, lp(-1, dp(1)))
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun shape(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }

    private fun read(key: String): JSONArray = try { JSONArray(prefs.getString(key, "[]")) } catch (_: Exception) { JSONArray() }
    private fun save(key: String, arr: JSONArray) { prefs.edit().putString(key, arr.toString()).apply() }
    private fun money(amount: Double): String = NumberFormat.getNumberInstance(Locale("ar", "YE")).apply { maximumFractionDigits = 2 }.format(amount)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun lp(w: Int, h: Int, l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0) =
        LinearLayout.LayoutParams(w, h).apply { setMargins(dp(l), dp(t), dp(r), dp(b)) }
    private fun spaceW(width: Int): View = View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(width), 1) }
    private fun spaceH(height: Int): View = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }
}
