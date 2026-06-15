package press.medya.app;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Build;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.text.Html;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.content.Intent;
import android.net.Uri;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends Activity {

    private static final String SITE_URL = "https://medya.press/";
    private static final String API_URL = "https://medya.press/wp-json/wp/v2/posts?_embed=1&per_page=30";
    private static final String THEME_ORANGE = "#f58220";
    private static final String THEME_BLACK = "#111111";
    private static final String BG = "#f5f5f5";

    private LinearLayout root;
    private LinearLayout content;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupWindow();
        buildLayout();
        loadPosts();
    }

    private void setupWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Color.parseColor(THEME_BLACK));
    }

    private void buildLayout() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.parseColor(BG));

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(26));
        root.setBackgroundColor(Color.parseColor(BG));

        scroll.addView(root);
        setContentView(scroll);

        addHeader();
        addNavigationChips();

        status = smallText("MedyaPress yükleniyor...", "#666666", 14);
        status.setPadding(dp(2), dp(14), dp(2), dp(10));
        root.addView(status);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content);
    }

    private void addHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18), dp(18), dp(18), dp(18));
        header.setBackground(round(THEME_BLACK, 28));
        header.setElevation(dp(3));

        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.setMargins(0, 0, 0, dp(12));
        root.addView(header, hp);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(top);

        ImageView brandLogo = new ImageView(this);
        brandLogo.setImageResource(R.drawable.medyapress_logo);
        brandLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        brandLogo.setAdjustViewBounds(true);

        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(66), dp(66));
        logoParams.setMargins(0, 0, dp(12), 0);
        top.addView(brandLogo, logoParams);

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));

        TextView logoText = new TextView(this);
        logoText.setText("MedyaPress");
        logoText.setTextColor(Color.WHITE);
        logoText.setTextSize(30);
        logoText.setTypeface(Typeface.DEFAULT_BOLD);
        logoText.setLetterSpacing(-0.03f);
        logoText.setSingleLine(true);
        logoText.setEllipsize(TextUtils.TruncateAt.END);
        brand.addView(logoText);

        TextView sub = new TextView(this);
        sub.setText("Küresel Dijital Haber Ağı");
        sub.setTextColor(Color.parseColor("#dddddd"));
        sub.setTextSize(14);
        sub.setPadding(0, dp(3), 0, 0);
        sub.setSingleLine(true);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        brand.addView(sub);

        TextView refresh = pill("YENİLE", THEME_ORANGE, "#ffffff", 14);
        refresh.setOnClickListener(v -> loadPosts());
        top.addView(refresh, new LinearLayout.LayoutParams(dp(92), dp(42)));

        TextView motto = new TextView(this);
        motto.setText("Dünyayı Sizin İçin Takip Ediyoruz!");
        motto.setTextColor(Color.parseColor("#f3f3f3"));
        motto.setTextSize(17);
        motto.setTypeface(Typeface.DEFAULT_BOLD);
        motto.setPadding(0, dp(18), 0, dp(4));
        header.addView(motto);

        TextView desc = new TextView(this);
        desc.setText("Son dakika gelişmeleri, gündem, dünya, yaşam, kültür-sanat ve özel röportajlar tek ekranda.");
        desc.setTextColor(Color.parseColor("#bdbdbd"));
        desc.setTextSize(13);
        desc.setLineSpacing(0, 1.15f);
        header.addView(desc);
    }

    private void addNavigationChips() {
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        hsv.addView(row);

        String[] chips = {"MANŞET", "GÜNDEM", "DÜNYA", "YAŞAM", "SPOR", "MAGAZİN", "RÖPORTAJ"};

        for (String c : chips) {
            TextView chip = pill(c, "#ffffff", "#222222", 13);
            chip.setBackground(round("#ffffff", 18, "#e2e2e2"));

            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, dp(38));
            cp.setMargins(0, 0, dp(8), 0);
            row.addView(chip, cp);
        }

        root.addView(hsv, new LinearLayout.LayoutParams(-1, dp(44)));
    }

    private void loadPosts() {
        if (content != null) {
            content.removeAllViews();
        }

        if (status != null) {
            status.setText("MedyaPress haber akışı güncelleniyor...");
        }

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... v) {
                return fetch(API_URL);
            }

            @Override
            protected void onPostExecute(String result) {
                if (result == null || result.trim().isEmpty()) {
                    status.setText("Haber akışı alınamadı. İnternet bağlantınızı kontrol edin.");
                    return;
                }

                try {
                    JSONArray posts = new JSONArray(result);
                    renderHome(posts);
                } catch (Exception e) {
                    status.setText("Haber verisi okunamadı.");
                }
            }
        }.execute();
    }

    private String fetch(String src) {
        HttpsURLConnection con = null;
        BufferedReader br = null;

        try {
            URL url = new URL(src);
            con = (HttpsURLConnection) url.openConnection();
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("User-Agent", "MedyaPressAndroid/1.6");
            con.setConnectTimeout(10000);
            con.setReadTimeout(18000);

            br = new BufferedReader(new InputStreamReader(con.getInputStream()));

            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            return sb.toString();

        } catch (Exception e) {
            return null;

        } finally {
            try {
                if (br != null) br.close();
            } catch (Exception ignored) {}

            if (con != null) {
                con.disconnect();
            }
        }
    }

    private void renderHome(JSONArray posts) {
        content.removeAllViews();

        if (posts.length() == 0) {
            status.setText("Şu anda gösterilecek haber bulunamadı.");
            return;
        }

        status.setText("MedyaPress ana sayfası güncellendi · " + posts.length() + " haber");

        try {
            JSONObject first = posts.getJSONObject(0);
            addHero(first);
        } catch (Exception ignored) {}

        addBreakingStrip(posts);
        addHorizontalSection("Son Haberler", posts, 1, Math.min(posts.length(), 9));
        addEditorialSection(posts);
        addCategorySections(posts);
        addFooter();
    }

    private void addHero(JSONObject p) {
        String title = postTitle(p);
        String excerpt = postExcerpt(p);
        String image = postImage(p);
        String category = postCategory(p);
        String link = postLink(p);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(round(THEME_BLACK, 28));
        card.setElevation(dp(4));
        card.setOnClickListener(v -> openLink(link));

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, dp(4), 0, dp(16));
        content.addView(card, cp);

        if (!TextUtils.isEmpty(image)) {
            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setBackgroundColor(Color.parseColor("#dddddd"));
            card.addView(img, new LinearLayout.LayoutParams(-1, dp(235)));
            new ImageTask(img).execute(image);
        }

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(16), dp(18), dp(18));
        card.addView(body);

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        body.addView(meta);

        TextView badge = pill("MANŞET", THEME_ORANGE, "#ffffff", 13);
        meta.addView(badge, new LinearLayout.LayoutParams(dp(92), dp(34)));

        TextView cat = smallText(category.toUpperCase(), "#cccccc", 12);
        cat.setTypeface(Typeface.DEFAULT_BOLD);
        cat.setPadding(dp(10), 0, 0, 0);
        meta.addView(cat);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(Color.WHITE);
        t.setTextSize(28);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(14), 0, dp(8));
        t.setLineSpacing(0, 1.04f);
        body.addView(t);

        TextView e = new TextView(this);
        e.setText(cut(excerpt, 175));
        e.setTextColor(Color.parseColor("#e0e0e0"));
        e.setTextSize(16);
        e.setLineSpacing(0, 1.15f);
        body.addView(e);

        TextView open = pill("HABERİ OKU", THEME_ORANGE, "#ffffff", 15);
        open.setOnClickListener(v -> openLink(link));

        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(-1, dp(48));
        op.setMargins(0, dp(16), 0, 0);
        body.addView(open, op);
    }

    private void addBreakingStrip(JSONArray posts) {
        TextView title = sectionTitle("Son Dakika Akışı");
        content.addView(title);

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        hsv.addView(row);

        int max = Math.min(posts.length(), 7);

        for (int i = 1; i < max; i++) {
            try {
                JSONObject p = posts.getJSONObject(i);
                String link = postLink(p);

                LinearLayout chip = new LinearLayout(this);
                chip.setOrientation(LinearLayout.VERTICAL);
                chip.setPadding(dp(14), dp(12), dp(14), dp(12));
                chip.setBackground(round("#ffffff", 18, "#e6e6e6"));
                chip.setOnClickListener(v -> openLink(link));

                TextView label = smallText("MEDYAPRESS", THEME_ORANGE, 11);
                label.setTypeface(Typeface.DEFAULT_BOLD);
                chip.addView(label);

                TextView ct = smallText(cut(postTitle(p), 58), "#222222", 15);
                ct.setTypeface(Typeface.DEFAULT_BOLD);
                ct.setMaxLines(3);
                chip.addView(ct);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(230), -2);
                lp.setMargins(0, 0, dp(10), dp(12));
                row.addView(chip, lp);

            } catch (Exception ignored) {}
        }

        content.addView(hsv);
    }

    private void addHorizontalSection(String title, JSONArray posts, int start, int end) {
        content.addView(sectionTitle(title));

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        hsv.addView(row);

        for (int i = start; i < end; i++) {
            try {
                JSONObject p = posts.getJSONObject(i);
                row.addView(horizontalCard(p));
            } catch (Exception ignored) {}
        }

        content.addView(hsv);
    }

    private View horizontalCard(JSONObject p) {
        String title = postTitle(p);
        String image = postImage(p);
        String cat = postCategory(p);
        String link = postLink(p);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(round("#ffffff", 22, "#e8e8e8"));
        card.setElevation(dp(2));
        card.setOnClickListener(v -> openLink(link));

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(dp(255), -2);
        cp.setMargins(0, 0, dp(12), dp(14));
        card.setLayoutParams(cp);

        if (!TextUtils.isEmpty(image)) {
            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setBackgroundColor(Color.parseColor("#dddddd"));
            card.addView(img, new LinearLayout.LayoutParams(-1, dp(150)));
            new ImageTask(img).execute(image);
        }

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14), dp(12), dp(14), dp(14));
        card.addView(body);

        TextView c = smallText(cat.toUpperCase(), THEME_ORANGE, 11);
        c.setTypeface(Typeface.DEFAULT_BOLD);
        body.addView(c);

        TextView t = smallText(title, "#171717", 18);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setMaxLines(3);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setPadding(0, dp(6), 0, 0);
        body.addView(t);

        return card;
    }

    private void addEditorialSection(JSONArray posts) {
        content.addView(sectionTitle("Editörün Seçtikleri"));

        int added = 0;

        for (int i = 2; i < posts.length() && added < 5; i++) {
            try {
                addListCard(posts.getJSONObject(i), added == 0);
                added++;
            } catch (Exception ignored) {}
        }
    }

    private void addCategorySections(JSONArray posts) {
        LinkedHashMap<String, ArrayList<JSONObject>> groups = new LinkedHashMap<>();

        for (int i = 0; i < posts.length(); i++) {
            try {
                JSONObject p = posts.getJSONObject(i);
                String cat = postCategory(p);

                if (TextUtils.isEmpty(cat)) cat = "Haber";
                if (cat.equalsIgnoreCase("Genel") || cat.equalsIgnoreCase("Uncategorized")) cat = "Haber";

                if (!groups.containsKey(cat)) {
                    groups.put(cat, new ArrayList<>());
                }

                groups.get(cat).add(p);

            } catch (Exception ignored) {}
        }

        int sectionCount = 0;

        for (Map.Entry<String, ArrayList<JSONObject>> entry : groups.entrySet()) {
            if (sectionCount >= 4) break;

            String cat = entry.getKey();
            ArrayList<JSONObject> list = entry.getValue();

            if (list.size() < 2) continue;

            content.addView(sectionTitle(cat));

            int max = Math.min(list.size(), 4);
            for (int i = 0; i < max; i++) {
                addCompactCard(list.get(i));
            }

            sectionCount++;
        }
    }

    private void addListCard(JSONObject p, boolean large) {
        String title = postTitle(p);
        String excerpt = postExcerpt(p);
        String image = postImage(p);
        String cat = postCategory(p);
        String link = postLink(p);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(round("#ffffff", 22, "#e8e8e8"));
        card.setElevation(dp(2));
        card.setOnClickListener(v -> openLink(link));

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, 0, 0, dp(14));
        content.addView(card, cp);

        if (!TextUtils.isEmpty(image)) {
            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setBackgroundColor(Color.parseColor("#dddddd"));
            card.addView(img, new LinearLayout.LayoutParams(-1, large ? dp(205) : dp(165)));
            new ImageTask(img).execute(image);
        }

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(14), dp(16), dp(16));
        card.addView(body);

        TextView c = smallText(cat.toUpperCase(), THEME_ORANGE, 11);
        c.setTypeface(Typeface.DEFAULT_BOLD);
        body.addView(c);

        TextView t = smallText(title, "#151515", large ? 23 : 20);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(7), 0, dp(7));
        t.setLineSpacing(0, 1.06f);
        body.addView(t);

        TextView e = smallText(cut(excerpt, large ? 155 : 120), "#666666", 15);
        e.setLineSpacing(0, 1.12f);
        body.addView(e);

        TextView open = pill("Haberi Aç", "#f1f1f1", "#222222", 14);
        open.setOnClickListener(v -> openLink(link));

        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(-1, dp(44));
        op.setMargins(0, dp(12), 0, 0);
        body.addView(open, op);
    }

    private void addCompactCard(JSONObject p) {
        String title = postTitle(p);
        String image = postImage(p);
        String cat = postCategory(p);
        String link = postLink(p);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        card.setBackground(round("#ffffff", 18, "#e8e8e8"));
        card.setElevation(dp(1));
        card.setOnClickListener(v -> openLink(link));

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, 0, 0, dp(10));
        content.addView(card, cp);

        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setBackgroundColor(Color.parseColor("#dddddd"));
        card.addView(img, new LinearLayout.LayoutParams(dp(96), dp(76)));

        if (!TextUtils.isEmpty(image)) {
            new ImageTask(img).execute(image);
        }

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(12), 0, 0, 0);
        card.addView(body, new LinearLayout.LayoutParams(0, -2, 1));

        TextView c = smallText(cat.toUpperCase(), THEME_ORANGE, 10);
        c.setTypeface(Typeface.DEFAULT_BOLD);
        body.addView(c);

        TextView t = smallText(title, "#161616", 17);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setMaxLines(3);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setPadding(0, dp(4), 0, 0);
        body.addView(t);
    }

    private void addFooter() {
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(dp(16), dp(22), dp(16), dp(22));
        footer.setBackground(round(THEME_BLACK, 24));

        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.setMargins(0, dp(18), 0, 0);
        content.addView(footer, fp);

        ImageView footerLogo = new ImageView(this);
        footerLogo.setImageResource(R.drawable.medyapress_logo);
        footerLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        footer.addView(footerLogo, new LinearLayout.LayoutParams(dp(70), dp(70)));

        TextView logo = smallText("MedyaPress", "#ffffff", 24);
        logo.setTypeface(Typeface.DEFAULT_BOLD);
        logo.setGravity(Gravity.CENTER);
        logo.setPadding(0, dp(8), 0, 0);
        footer.addView(logo);

        TextView slogan = smallText("Dünyayı Sizin İçin Takip Ediyoruz!", "#cccccc", 14);
        slogan.setGravity(Gravity.CENTER);
        slogan.setPadding(0, dp(6), 0, dp(12));
        footer.addView(slogan);

        TextView site = pill("medya.press", THEME_ORANGE, "#ffffff", 14);
        site.setOnClickListener(v -> openLink(SITE_URL));
        footer.addView(site, new LinearLayout.LayoutParams(dp(150), dp(42)));
    }

    private TextView sectionTitle(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(22);
        t.setTextColor(Color.parseColor("#111111"));
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(dp(2), dp(18), dp(2), dp(10));
        return t;
    }

    private TextView smallText(String s, String color, int size) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(Color.parseColor(color));
        t.setTextSize(size);
        return t;
    }

    private TextView pill(String s, String bg, String color, int size) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        t.setTextColor(Color.parseColor(color));
        t.setBackground(round(bg, 18));
        t.setSingleLine(true);
        return t;
    }

    private String postTitle(JSONObject p) {
        try {
            return clean(p.getJSONObject("title").optString("rendered", "Başlıksız"));
        } catch (Exception e) {
            return "Başlıksız";
        }
    }

    private String postExcerpt(JSONObject p) {
        try {
            return clean(p.getJSONObject("excerpt").optString("rendered", ""));
        } catch (Exception e) {
            return "";
        }
    }

    private String postLink(JSONObject p) {
        return p.optString("link", SITE_URL);
    }

    private String postImage(JSONObject p) {
        try {
            JSONObject embedded = p.optJSONObject("_embedded");
            if (embedded == null) return "";

            JSONArray media = embedded.optJSONArray("wp:featuredmedia");
            if (media == null || media.length() == 0) return "";

            JSONObject item = media.getJSONObject(0);
            JSONObject details = item.optJSONObject("media_details");

            if (details != null) {
                JSONObject sizes = details.optJSONObject("sizes");

                if (sizes != null) {
                    if (sizes.has("large")) {
                        return sizes.getJSONObject("large").optString("source_url", "");
                    }

                    if (sizes.has("medium_large")) {
                        return sizes.getJSONObject("medium_large").optString("source_url", "");
                    }

                    if (sizes.has("medium")) {
                        return sizes.getJSONObject("medium").optString("source_url", "");
                    }
                }
            }

            return item.optString("source_url", "");

        } catch (Exception e) {
            return "";
        }
    }

    private String postCategory(JSONObject p) {
        try {
            JSONObject embedded = p.optJSONObject("_embedded");
            if (embedded == null) return "Haber";

            JSONArray termsOuter = embedded.optJSONArray("wp:term");
            if (termsOuter == null) return "Haber";

            for (int i = 0; i < termsOuter.length(); i++) {
                JSONArray group = termsOuter.optJSONArray(i);
                if (group == null) continue;

                for (int j = 0; j < group.length(); j++) {
                    JSONObject term = group.optJSONObject(j);
                    if (term == null) continue;

                    String taxonomy = term.optString("taxonomy", "");
                    String name = term.optString("name", "");

                    if ("category".equals(taxonomy) && !TextUtils.isEmpty(name)) {
                        return clean(name);
                    }
                }
            }

            return "Haber";

        } catch (Exception e) {
            return "Haber";
        }
    }

    private String clean(String html) {
        if (html == null) return "";

        String text;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            text = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            text = Html.fromHtml(html).toString();
        }

        return text.replaceAll("\\s+", " ").trim();
    }

    private String cut(String s, int len) {
        if (s == null) return "";

        String cleaned = s.trim();

        if (cleaned.length() <= len) {
            return cleaned;
        }

        return cleaned.substring(0, len).trim() + "...";
    }

    private GradientDrawable round(String color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.parseColor(color));
        g.setCornerRadius(dp(radius));
        return g;
    }

    private GradientDrawable round(String color, int radius, String stroke) {
        GradientDrawable g = round(color, radius);
        g.setStroke(dp(1), Color.parseColor(stroke));
        return g;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private void openLink(String link) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
        } catch (Exception ignored) {}
    }

    private class ImageTask extends AsyncTask<String, Void, Bitmap> {
        private final ImageView iv;

        ImageTask(ImageView iv) {
            this.iv = iv;
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            InputStream input = null;

            try {
                input = new URL(urls[0]).openStream();
                return BitmapFactory.decodeStream(input);

            } catch (Exception e) {
                return null;

            } finally {
                try {
                    if (input != null) input.close();
                } catch (Exception ignored) {}
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                iv.setImageBitmap(bitmap);
            }
        }
    }
}
