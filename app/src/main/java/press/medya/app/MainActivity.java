package press.medya.app;

import android.app.Activity;
import android.os.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.text.*;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.content.*;
import android.net.Uri;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends Activity {
    private static final String APP_NAME="MedyaPress Mobil Uygulaması";
    private static final String SITE_URL="https://medya.press/";
    private static final String API_URL="https://medya.press/wp-json/wp/v2/posts?_embed=1&per_page=20";
    private static final String CAT_URL="https://medya.press/wp-json/wp/v2/categories?per_page=100";
    private static final String THEME_COLOR="#f58220";
    private LinearLayout root,list,categoryRow;
    private TextView status;
    private EditText search;
    private JSONArray allPosts=new JSONArray();
    private String activeCategory="";
    private final String[] categorySlugs={"","gundem","dunya","spor","magazin","teknoloji","roportajlar","yazarlar"};
    private final String[] categoryLabels={"Son Haberler","Gündem","Dünya","Spor","Magazin","Teknoloji","Röportaj","Yazarlar"};
    private final Map<String,Integer> categoryMap=new HashMap<>();

    public void onCreate(Bundle b){ super.onCreate(b); buildLayout(); loadCategories(); loadPosts(""); }

    private void buildLayout(){
        ScrollView scroll=new ScrollView(this);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(14),dp(14),dp(14),dp(28)); root.setBackgroundColor(Color.rgb(247,247,247)); scroll.addView(root); setContentView(scroll);
        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(0,dp(6),0,dp(14)); root.addView(top);
        TextView logo=new TextView(this); logo.setText("MP"); logo.setTextColor(Color.WHITE); logo.setTypeface(Typeface.DEFAULT_BOLD); logo.setTextSize(22); logo.setGravity(Gravity.CENTER); logo.setBackground(round(THEME_COLOR,16)); top.addView(logo,new LinearLayout.LayoutParams(dp(58),dp(58)));
        LinearLayout brand=new LinearLayout(this); brand.setOrientation(LinearLayout.VERTICAL); brand.setPadding(dp(12),0,0,0); top.addView(brand,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
        TextView title=new TextView(this); title.setText(APP_NAME); title.setTextColor(Color.rgb(18,18,18)); title.setTextSize(26); title.setTypeface(Typeface.DEFAULT_BOLD); brand.addView(title);
        TextView slogan=new TextView(this); slogan.setText("Dünyayı Sizin İçin Takip Ediyoruz!"); slogan.setTextColor(Color.rgb(96,96,96)); slogan.setTextSize(13); brand.addView(slogan);
        TextView refresh=button("YENİLE","#ffffff",Color.rgb(20,20,20)); refresh.setOnClickListener(v->loadPosts(activeCategory)); top.addView(refresh,new LinearLayout.LayoutParams(dp(92),dp(46)));

        search=new EditText(this); search.setHint("Haber ara..."); search.setSingleLine(true); search.setImeOptions(EditorInfo.IME_ACTION_SEARCH); search.setTextSize(18); search.setPadding(dp(18),0,dp(18),0); search.setBackground(round("#ffffff",18,"#ececec")); root.addView(search,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(56)));
        search.setOnEditorActionListener((v,a,e)->{filterPosts(search.getText().toString());return false;});

        HorizontalScrollView hsv=new HorizontalScrollView(this); hsv.setHorizontalScrollBarEnabled(false); categoryRow=new LinearLayout(this); categoryRow.setPadding(0,dp(12),0,dp(12)); hsv.addView(categoryRow); root.addView(hsv); renderCategoryButtons();
        status=new TextView(this); status.setText("Haberler yükleniyor..."); status.setTextColor(Color.rgb(85,85,85)); status.setTextSize(14); status.setPadding(0,dp(2),0,dp(8)); root.addView(status);
        list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); root.addView(list);
    }

    private void renderCategoryButtons(){
        categoryRow.removeAllViews();
        for(int i=0;i<categoryLabels.length;i++){
            String slug=categorySlugs[i];
            TextView chip=new TextView(this); chip.setText(categoryLabels[i]); chip.setTextSize(13); chip.setTypeface(Typeface.DEFAULT_BOLD); chip.setGravity(Gravity.CENTER); chip.setPadding(dp(14),dp(9),dp(14),dp(9));
            boolean active=slug.equals(activeCategory); chip.setTextColor(active?Color.WHITE:Color.rgb(40,40,40)); chip.setBackground(round(active?THEME_COLOR:"#ffffff",99,active?THEME_COLOR:"#e8e8e8"));
            chip.setOnClickListener(v->{ activeCategory=slug; renderCategoryButtons(); loadPosts(slug); });
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT); lp.setMargins(0,0,dp(8),0); categoryRow.addView(chip,lp);
        }
    }

    private void loadCategories(){ new AsyncTask<Void,Void,String>(){ protected String doInBackground(Void...v){return get(CAT_URL);} protected void onPostExecute(String r){ if(r==null)return; try{JSONArray cats=new JSONArray(r); for(int i=0;i<cats.length();i++){JSONObject c=cats.getJSONObject(i); categoryMap.put(c.optString("slug"),c.optInt("id"));}}catch(Exception ignored){} }}.execute(); }

    private void loadPosts(String slug){
        status.setText("Haberler yükleniyor..."); list.removeAllViews();
        String url=API_URL; if(!TextUtils.isEmpty(slug)&&categoryMap.containsKey(slug)) url+="&categories="+categoryMap.get(slug);
        final String finalUrl=url;
        new AsyncTask<Void,Void,String>(){ protected String doInBackground(Void...v){return get(finalUrl);} protected void onPostExecute(String r){ if(r==null){status.setText("Haber akışı alınamadı.");return;} try{ allPosts=new JSONArray(r); renderPosts(allPosts);}catch(Exception e){status.setText("Haber verisi okunamadı.");} }}.execute();
    }

    private String get(String src){ try{ URL url=new URL(src); HttpsURLConnection con=(HttpsURLConnection)url.openConnection(); con.setRequestProperty("Accept","application/json"); con.setConnectTimeout(10000); con.setReadTimeout(18000); BufferedReader br=new BufferedReader(new InputStreamReader(con.getInputStream())); StringBuilder sb=new StringBuilder(); String line; while((line=br.readLine())!=null)sb.append(line); br.close(); return sb.toString(); }catch(Exception e){ return null; } }

    private void filterPosts(String q){ try{ if(TextUtils.isEmpty(q)){renderPosts(allPosts);return;} JSONArray f=new JSONArray(); String n=q.toLowerCase(); for(int i=0;i<allPosts.length();i++){JSONObject p=allPosts.getJSONObject(i); String t=clean(p.getJSONObject("title").optString("rendered","")); String e=clean(p.getJSONObject("excerpt").optString("rendered","")); if(t.toLowerCase().contains(n)||e.toLowerCase().contains(n))f.put(p);} renderPosts(f);}catch(Exception e){status.setText("Arama yapılamadı.");} }

    private void renderPosts(JSONArray posts){ list.removeAllViews(); status.setText(posts.length()+" haber gösteriliyor."); for(int i=0;i<posts.length();i++){ try{JSONObject p=posts.getJSONObject(i); String t=clean(p.getJSONObject("title").optString("rendered","Başlıksız")); String e=clean(p.getJSONObject("excerpt").optString("rendered","")); String link=p.optString("link",SITE_URL); String img=imageFromPost(p); if(i==0)addHero(t,e,link,img); else addCard(t,e,link,img);}catch(Exception ignored){} } }

    private String imageFromPost(JSONObject p){ try{ JSONObject emb=p.optJSONObject("_embedded"); if(emb==null)return ""; JSONArray media=emb.optJSONArray("wp:featuredmedia"); if(media==null||media.length()==0)return ""; return media.getJSONObject(0).optString("source_url",""); }catch(Exception e){return "";} }

    private void addHero(String title,String excerpt,String link,String imageUrl){
        LinearLayout hero=new LinearLayout(this); hero.setOrientation(LinearLayout.VERTICAL); hero.setBackground(round("#151515",24)); LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2); hp.setMargins(0,dp(8),0,dp(14)); list.addView(hero,hp);
        ImageView img=new ImageView(this); img.setScaleType(ImageView.ScaleType.CENTER_CROP); img.setBackgroundColor(Color.rgb(220,220,220)); hero.addView(img,new LinearLayout.LayoutParams(-1,dp(210))); if(!TextUtils.isEmpty(imageUrl))new ImageTask(img).execute(imageUrl);
        LinearLayout body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(16),dp(14),dp(16),dp(16)); hero.addView(body);
        TextView badge=button("ÖNE ÇIKAN",THEME_COLOR,Color.WHITE); body.addView(badge,new LinearLayout.LayoutParams(dp(120),dp(34)));
        TextView t=text(title,24,Color.WHITE,true); t.setPadding(0,dp(10),0,dp(6)); body.addView(t);
        TextView e=text(cut(excerpt,140),14,Color.rgb(220,220,220),false); body.addView(e);
        TextView open=button("HABERİ AÇ",THEME_COLOR,Color.WHITE); open.setOnClickListener(v->openLink(link)); LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(-1,dp(48)); op.setMargins(0,dp(12),0,0); body.addView(open,op);
    }

    private void addCard(String title,String excerpt,String link,String imageUrl){
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setBackground(round("#ffffff",20,"#eeeeee")); LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.setMargins(0,0,0,dp(14)); list.addView(card,cp);
        if(!TextUtils.isEmpty(imageUrl)){ ImageView img=new ImageView(this); img.setScaleType(ImageView.ScaleType.CENTER_CROP); img.setBackgroundColor(Color.rgb(235,235,235)); card.addView(img,new LinearLayout.LayoutParams(-1,dp(175))); new ImageTask(img).execute(imageUrl); }
        LinearLayout body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(16),dp(14),dp(16),dp(16)); card.addView(body);
        body.addView(text(title,20,Color.rgb(20,20,20),true));
        TextView e=text(cut(excerpt,135),15,Color.rgb(95,95,95),false); e.setPadding(0,dp(8),0,dp(12)); body.addView(e);
        TextView open=button("HABERİ AÇ","#eeeeee",Color.rgb(20,20,20)); open.setOnClickListener(v->openLink(link)); body.addView(open,new LinearLayout.LayoutParams(-1,dp(44)));
    }

    private TextView text(String s,int size,int color,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); if(bold)t.setTypeface(Typeface.DEFAULT_BOLD); return t; }
    private TextView button(String s,String bg,int color){ TextView b=new TextView(this); b.setText(s); b.setTextSize(15); b.setTypeface(Typeface.DEFAULT_BOLD); b.setGravity(Gravity.CENTER); b.setTextColor(color); b.setBackground(round(bg,12)); return b; }
    private void openLink(String link){ try{startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));}catch(Exception ignored){} }
    private String clean(String html){ if(html==null)return ""; return Html.fromHtml(html,Html.FROM_HTML_MODE_LEGACY).toString().replaceAll("\\\\s+"," ").trim(); }
    private String cut(String s,int len){ if(s==null)return ""; return s.length()>len?s.substring(0,len)+"...":s; }
    private GradientDrawable round(String color,int radius){ GradientDrawable g=new GradientDrawable(); g.setColor(Color.parseColor(color)); g.setCornerRadius(dp(radius)); return g; }
    private GradientDrawable round(String color,int radius,String stroke){ GradientDrawable g=round(color,radius); g.setStroke(1,Color.parseColor(stroke)); return g; }
    private int dp(int v){ return (int)(v*getResources().getDisplayMetrics().density); }

    private class ImageTask extends AsyncTask<String,Void,Bitmap>{
        private final ImageView iv; ImageTask(ImageView i){iv=i;}
        protected Bitmap doInBackground(String...u){ try{InputStream input=new URL(u[0]).openStream(); return BitmapFactory.decodeStream(input);}catch(Exception e){return null;} }
        protected void onPostExecute(Bitmap b){ if(b!=null)iv.setImageBitmap(b); }
    }
}
