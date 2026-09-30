package com.example.filmdownloader

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private lateinit var titleInput: EditText
    private lateinit var yearInput: EditText
    private lateinit var minInput: EditText
    private lateinit var maxInput: EditText
    private lateinit var status: TextView

    private val loadix = "https://loadix.fun/"
    private val searchUrl = "https://loadix.fun/search?provider=1fichier&type=movie"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        web = findViewById(R.id.webView)
        titleInput = findViewById(R.id.titleInput)
        yearInput = findViewById(R.id.yearInput)
        minInput = findViewById(R.id.minInput)
        maxInput = findViewById(R.id.maxInput)
        status = findViewById(R.id.statusText)

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.databaseEnabled = true
        web.settings.userAgentString = web.settings.userAgentString
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = false
            override fun onPageFinished(view: WebView, url: String) {
                status.text = when {
                    url.contains("1fichier.com") -> "1Fichier ouvert. Terminez manuellement toute confirmation nécessaire."
                    url.contains("/media/") -> "Fiche du film ouverte. Recherche de la release compatible…"
                    url.contains("loadix.fun") -> "Loadix chargé."
                    else -> url
                }
            }
        }

        findViewById<Button>(R.id.openLoadix).setOnClickListener {
            web.loadUrl(loadix)
            status.text = "Connectez-vous manuellement à Loadix et effectuez vous-même toute vérification anti-bot."
        }

        findViewById<Button>(R.id.searchButton).setOnClickListener { beginSearch() }

        findViewById<Button>(R.id.captchaDone).setOnClickListener {
            status.text = "Vérification manuelle confirmée. Recherche du lien révélé…"
            findOneFichierLink()
        }

        web.loadUrl(loadix)
    }

    private fun parseGiB(text: String): Double? {
        val s = text.lowercase(Locale.ROOT).replace(",", ".").trim()
        val m = Regex("""(\d+(?:\.\d+)?)\s*(ko|mo|go|to|kb|mb|gb|tb)?""").matchEntire(s) ?: return null
        val v = m.groupValues[1].toDoubleOrNull() ?: return null
        return when (m.groupValues[2].ifBlank { "go" }) {
            "ko","kb" -> v / (1024.0 * 1024.0)
            "mo","mb" -> v / 1024.0
            "go","gb" -> v
            "to","tb" -> v * 1024.0
            else -> null
        }
    }

    private fun compact(s: String): String {
        val n = Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase(Locale.ROOT)
        return n.replace(Regex("[^a-z0-9]+"), "")
    }

    private fun beginSearch() {
        val title = titleInput.text.toString().trim()
        if (title.isBlank()) {
            Toast.makeText(this, "Le titre est obligatoire.", Toast.LENGTH_SHORT).show()
            return
        }
        val lo = minInput.text.toString().takeIf { it.isNotBlank() }?.let(::parseGiB)
        val hi = maxInput.text.toString().takeIf { it.isNotBlank() }?.let(::parseGiB)
        if (minInput.text.isNotBlank() && lo == null || maxInput.text.isNotBlank() && hi == null) {
            Toast.makeText(this, "Taille invalide. Exemple : 2 Go", Toast.LENGTH_LONG).show()
            return
        }
        if (lo != null && hi != null && lo > hi) {
            Toast.makeText(this, "La taille minimale dépasse la maximale.", Toast.LENGTH_LONG).show()
            return
        }

        status.text = "Ouverture de la recherche Loadix…"
        web.loadUrl(searchUrl)
        web.postDelayed({ injectSearch(title, yearInput.text.toString().trim()) }, 1800)
    }

    private fun injectSearch(title: String, year: String) {
        val q = (title + if (year.isBlank()) "" else " $year").trim()
        val js = """
            (() => {
              const q=${JSONObject.quote(q)};
              const inputs=[...document.querySelectorAll('input')];
              const input=inputs.find(x => /recher|search/i.test((x.placeholder||'')+' '+(x.name||'')))
                       || inputs.find(x => ['text','search'].includes((x.type||'text').toLowerCase()));
              if(!input) return 'NO_INPUT';
              input.focus(); input.value=q;
              input.dispatchEvent(new Event('input',{bubbles:true}));
              input.dispatchEvent(new Event('change',{bubbles:true}));
              const buttons=[...document.querySelectorAll('button,input[type=submit]')];
              const b=buttons.find(x => /recher|search/i.test((x.innerText||x.value||'')));
              if(b){ b.click(); return 'CLICKED'; }
              input.dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',code:'Enter',bubbles:true}));
              return 'ENTER';
            })()
        """.trimIndent()
        web.evaluateJavascript(js) {
            status.text = "Recherche envoyée. Analyse des résultats…"
            web.postDelayed({ findMovieCard(title, year) }, 1800)
        }
    }

    private fun findMovieCard(title: String, year: String) {
        val target = compact(title)
        val js = """
          (() => {
            const norm=s=>(s||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'')
              .toLowerCase().replace(/[^a-z0-9]+/g,'');
            const target=${JSONObject.quote(target)};
            const year=${JSONObject.quote(year)};
            let best=null, bestScore=-1;
            for(const a of document.querySelectorAll('a[href*="/media/"]')){
              let node=a, text='';
              for(let i=0;i<6 && node;i++,node=node.parentElement){
                const t=(node.innerText||'').trim();
                if(t && t.length<1600) text=t;
              }
              const n=norm(text);
              let score=0;
              if(n.includes(target)) score+=100;
              for(const tok of target.match(/[a-z]+|\d+/g)||[]) if(tok.length>=3 && n.includes(tok)) score+=5;
              if(year && text.includes(year)) score+=10;
              if(score>bestScore){bestScore=score;best={href:a.href,text,score};}
            }
            return best ? JSON.stringify(best) : '';
          })()
        """.trimIndent()

        web.evaluateJavascript(js) { raw ->
            val decoded = decodeJsString(raw)
            if (decoded.isBlank()) {
                status.text = "Aucune fiche de film suffisamment proche trouvée."
                return@evaluateJavascript
            }
            val obj = JSONObject(decoded)
            status.text = "Fiche trouvée. Ouverture…"
            web.loadUrl(obj.getString("href"))
            web.postDelayed({ chooseNewestRelease(title) }, 2000)
        }
    }

    private fun chooseNewestRelease(title: String) {
        val lo = minInput.text.toString().takeIf { it.isNotBlank() }?.let(::parseGiB)
        val hi = maxInput.text.toString().takeIf { it.isNotBlank() }?.let(::parseGiB)
        val js = """
          (() => {
            const lo=${lo ?: "null"}, hi=${hi ?: "null"};
            const sizeOf=t=>{
              const m=(t||'').match(/(\d+(?:[.,]\d+)?)\s*(Go|GB|Mo|MB|To|TB)\b/i);
              if(!m)return null;
              let v=parseFloat(m[1].replace(',','.')), u=m[2].toLowerCase();
              if(u==='mo'||u==='mb')v/=1024;
              if(u==='to'||u==='tb')v*=1024;
              return v;
            };
            const dateOf=t=>{
              let m=(t||'').match(/(\d{1,2})[\/.-](\d{1,2})[\/.-](20\d{2})(?:\s+(?:à\s*)?(\d{1,2})[:h](\d{2}))?/i);
              if(m)return new Date(+m[3],+m[2]-1,+m[1],+(m[4]||0),+(m[5]||0)).getTime();
              return 0;
            };
            const controls=[...document.querySelectorAll('button,a')].filter(x=>/^\s*révéler\s*$/i.test((x.innerText||'').trim()));
            const rows=[];
            controls.forEach((btn,i)=>{
              let n=btn, best=null;
              for(let d=0;d<8 && n;d++,n=n.parentElement){
                const t=(n.innerText||'').trim();
                if(/1fichier/i.test(t) && sizeOf(t)!=null){best=n;break;}
              }
              if(!best)return;
              const text=best.innerText||'', size=sizeOf(text);
              if(lo!=null && (size==null||size<lo))return;
              if(hi!=null && (size==null||size>hi))return;
              rows.push({btn,best,text,size,date:dateOf(text)});
            });
            if(!rows.length)return 'NONE';
            rows.sort((a,b)=>b.date-a.date);
            const r=rows[0];
            const selector=r.best.querySelector('input[type=checkbox],input[type=radio],[role=checkbox],[role=radio]');
            if(selector && !selector.checked) selector.click();
            else {
              const label=r.best.querySelector('label');
              if(label) label.click();
            }
            setTimeout(()=>{
              const enabled=[...document.querySelectorAll('button')].find(x=>/^\s*révéler\s*$/i.test((x.innerText||'').trim()) && !x.disabled);
              (enabled||r.btn).click();
            },400);
            return JSON.stringify({text:r.text.slice(0,300),size:r.size,date:r.date});
          })()
        """.trimIndent()

        web.evaluateJavascript(js) { raw ->
            val decoded = decodeJsString(raw)
            if (decoded == "NONE" || decoded.isBlank()) {
                status.text = "Aucune release 1Fichier compatible avec les critères."
            } else {
                status.text = "Release la plus récente sélectionnée. Dans la fenêtre Loadix, effectuez manuellement le CAPTCHA/vérification puis touchez « CAPTCHA terminé » ici."
            }
        }
    }

    private fun findOneFichierLink() {
        val js = """
          (() => {
            const a=[...document.querySelectorAll('a[href]')].find(x=>/1fichier\.com/i.test(x.href||''));
            if(a){ location.href=a.href; return a.href; }
            const b=[...document.querySelectorAll('button,a')].find(x=>/révéler\s+le\s+lien/i.test((x.innerText||'').trim()) && !x.disabled);
            if(b){ b.click(); return 'RECLICK'; }
            return '';
          })()
        """.trimIndent()
        web.evaluateJavascript(js) { raw ->
            val result = decodeJsString(raw)
            if (result.startsWith("http")) {
                status.text = "Lien 1Fichier ouvert."
            } else if (result == "RECLICK") {
                status.text = "Révélation relancée après votre validation. Recherche du lien…"
                web.postDelayed({ findOneFichierLink() }, 1800)
            } else {
                status.text = "Aucun lien 1Fichier visible pour l'instant. Vérifiez la fenêtre Loadix puis réessayez."
            }
        }
    }

    private fun decodeJsString(raw: String?): String {
        if (raw == null || raw == "null") return ""
        return try {
            if (raw.startsWith("\"")) JSONObject("{\"v\":$raw}").getString("v") else raw
        } catch (_: Exception) { raw.trim('"') }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}
