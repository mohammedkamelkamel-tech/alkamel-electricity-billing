package ye.alkamel.billing

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("alkamel_local_db", MODE_PRIVATE) }
    private lateinit var list: LinearLayout
    private lateinit var nameInput: EditText
    private lateinit var amountInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(20, 70, 60)
        window.navigationBarColor = Color.BLACK
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 12)
            setBackgroundColor(Color.rgb(246, 248, 247))
        }
        root.addView(TextView(this).apply {
            text = "الكامل للفواتير والتحصيل"
            textSize = 23f
            setTextColor(Color.rgb(20, 70, 60))
            gravity = Gravity.CENTER
            setPadding(4, 12, 4, 18)
        }, matchWrap())
        nameInput = EditText(this).apply { hint = "اسم المشترك"; textSize = 16f; singleLine = true }
        root.addView(nameInput, matchWrap())
        amountInput = EditText(this).apply {
            hint = "قيمة الفاتورة (ريال)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            textSize = 16f
            singleLine = true
        }
        root.addView(amountInput, matchWrap())
        root.addView(Button(this).apply {
            text = "إضافة فاتورة"
            setOnClickListener { addInvoice() }
        }, matchWrap())
        root.addView(TextView(this).apply {
            text = "الفواتير المحفوظة على الجهاز"
            textSize = 18f
            setTextColor(Color.DKGRAY)
            setPadding(2, 18, 2, 8)
        }, matchWrap())
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        renderInvoices()
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun addInvoice() {
        val name = nameInput.text.toString().trim()
        val amount = amountInput.text.toString().trim().toDoubleOrNull()
        if (name.isEmpty() || amount == null || amount < 0) {
            AlertDialog.Builder(this).setTitle("بيانات غير مكتملة")
                .setMessage("أدخل اسم المشترك وقيمة فاتورة صحيحة.")
                .setPositiveButton("حسنًا", null).show()
            return
        }
        val invoices = readInvoices()
        invoices.put(JSONObject().apply {
            put("name", name)
            put("amount", amount)
            put("createdAt", System.currentTimeMillis())
        })
        prefs.edit().putString("invoices", invoices.toString()).apply()
        nameInput.text.clear()
        amountInput.text.clear()
        renderInvoices()
    }

    private fun readInvoices(): JSONArray = try {
        JSONArray(prefs.getString("invoices", "[]"))
    } catch (_: Exception) { JSONArray() }

    private fun renderInvoices() {
        list.removeAllViews()
        val invoices = readInvoices()
        if (invoices.length() == 0) {
            list.addView(TextView(this).apply {
                text = "لا توجد فواتير بعد. أضف أول فاتورة من الأعلى."
                textSize = 15f
                setTextColor(Color.GRAY)
                setPadding(8, 16, 8, 16)
            }, matchWrap())
            return
        }
        for (i in invoices.length() - 1 downTo 0) {
            val item = invoices.optJSONObject(i) ?: continue
            val date = SimpleDateFormat("yyyy-MM-dd  HH:mm", Locale.getDefault())
                .format(Date(item.optLong("createdAt")))
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(14, 12, 14, 12)
                setBackgroundColor(Color.WHITE)
            }
            card.addView(TextView(this).apply {
                text = item.optString("name")
                textSize = 18f
                setTextColor(Color.rgb(20, 70, 60))
            }, matchWrap())
            card.addView(TextView(this).apply {
                text = "قيمة الفاتورة: %.2f ريال".format(Locale.getDefault(), item.optDouble("amount"))
                textSize = 16f
                setTextColor(Color.DKGRAY)
            }, matchWrap())
            card.addView(TextView(this).apply {
                text = date
                textSize = 12f
                setTextColor(Color.GRAY)
            }, matchWrap())
            card.setOnLongClickListener {
                AlertDialog.Builder(this).setTitle("حذف الفاتورة")
                    .setMessage("هل تريد حذف فاتورة ${item.optString("name")}؟")
                    .setNegativeButton("إلغاء", null)
                    .setPositiveButton("حذف") { _, _ ->
                        val updated = readInvoices()
                        val rebuilt = JSONArray()
                        for (j in 0 until updated.length()) {
                            if (j != i) rebuilt.put(updated.getJSONObject(j))
                        }
                        prefs.edit().putString("invoices", rebuilt.toString()).apply()
                        renderInvoices()
                    }.show()
                true
            }
            list.addView(card, matchWrap().apply { bottomMargin = 10 })
        }
    }
}
