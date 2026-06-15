package press.medya.app;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.text.Html;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.content.Intent;
import android.net.Uri;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends Activity {
    private static final String APP_NAME = "MedyaPress Mobil Uygulaması";
    private static final String SITE_URL = "https://medya.press/";
    private static final String API_URL = "https://medya.press/wp-json/wp/v2/posts?_embed=1&per_page=20";
    private static final String THEME_COLOR = "#f58220";

    private LinearLayout list;
    private TextView status;
    private EditText search;
    private JSONArray allPosts = new JSONArray();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        loadPosts();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(30));
        root.setBackgroundColor(Color.rgb(247, 247, 247));

        scroll.addView(root);
        setContentView(scroll);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(0, 0, 0, dp(14));
        root.addView(top);

        TextView logo = new TextView(this);
        logo.setText("MP");
        logo.setTextColor(Color.WHITE);
        logo.setTypeface(Typeface.DEFAULT_BOLD);
        logo.setTextSize(22);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(round(THEME_COLOR, 16));
        top.addView(logo, new LinearLayout.LayoutParams(dp(58), dp(58)));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(dp(12), 0, 0, 0);
        top.addView(
                brand,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        TextView title = text(APP_NAME, 26, Color.rgb(18, 18, 18), true);
        brand.addView(title);

        TextView slogan = text("Dünyayı Sizin İçin Takip Ediyoruz!", 13, Color.rgb(96, 96, 96), false);
        brand.addView(slogan);

        TextView refresh = button("YENİLE", "#ffffff", Color.rgb(20, 20, 20));
        refresh.setOnClickListener(v -> loadPosts());
        top.addView(refresh, new LinearLayout.LayoutParams(dp(92), dp(46)));

        search = new EditText(this);
        search.setHint("Haber ara...");
        search.setSingleLine(true);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setTextSize(18);
        search.setPadding(dp(18), 0, dp(18), 0);
        search.setBackground(round("#ffffff", 18, "#ececec"));
        root.addView(
                search,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(56)
                )
        );

        search.setOnEditorActionListener((v, actionId, event) -> {
            filterPosts(search.getText().toString());
            return false;
        });

        status = text("Haberler yükleniyor...", 14, Color.rgb(85, 85, 85), false);
        status.setPadding(0, dp(12), 0, dp(8));
        root.addView(status);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);
    }

    private void loadPosts() {
        list.removeAllViews();
        status.setText("Haberler yükleniyor...");

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... v) {
                return fetch(API_URL);
            }

            @Override
            protected void onPostExecute(String result) {
                if (result == null) {
                    status.setText("Haber akışı alınamadı.");
                    return;
                }

                try {
                    allPosts = new JSONArray(result);
                    renderPosts(allPosts);
                } catch (Exception e) {
                    status.setText("Haber verisi okunamadı.");
                }
            }
        }.execute();
    }

    private String fetch(String src) {
        try {
            URL url = new URL(src);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

            con.setRequestProperty("Accept", "application/json");
            con.setConnectTimeout(10000);
            con.setReadTimeout(18000);

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream())
            );

            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            br.close();
            return sb.toString();

        } catch (Exception e) {
            return null;
        }
    }

    private void filterPosts(String q) {
        try {
            if (TextUtils.isEmpty(q)) {
                renderPosts(allPosts);
                return;
            }

            JSONArray filtered = new JSONArray();
            String needle = q.toLowerCase();

            for (int i = 0; i < allPosts.length(); i++) {
                JSONObject p = allPosts.getJSONObject(i);

                String title = clean(
                        p.getJSONObject("title").optString("rendered", "")
                );

                String excerpt = clean(
                        p.getJSONObject("excerpt").optString("rendered", "")
                );

                if (
                        title.toLowerCase().contains(needle)
                                || excerpt.toLowerCase().contains(needle)
                ) {
                    filtered.put(p);
                }
            }

            renderPosts(filtered);

        } catch (Exception e) {
            status.setText("Arama yapılamadı.");
        }
    }

    private void renderPosts(JSONArray posts) {
        list.removeAllViews();
        status.setText(posts.length() + " haber gösteriliyor.");

        for (int i = 0; i < posts.length(); i++) {
            try {
                JSONObject p = posts.getJSONObject(i);

                String title = clean(
                        p.getJSONObject("title").optString("rendered", "Başlıksız")
                );

                String excerpt = clean(
                        p.getJSONObject("excerpt").optString("rendered", "")
                );

                String link = p.optString("link", SITE_URL);
                String image = imageFromPost(p);

                if (i == 0) {
                    addHero(title, excerpt, link, image);
                } else {
                    addCard(title, excerpt, link, image);
                }

            } catch (Exception ignored) {
            }
        }
    }

    private String imageFromPost(JSONObject p) {
        try {
            JSONObject embedded = p.optJSONObject("_embedded");

            if (embedded == null) {
                return "";
            }

            JSONArray media = embedded.optJSONArray("wp:featuredmedia");

            if (media == null || media.length() == 0) {
                return "";
            }

            return media.getJSONObject(0).optString("source_url", "");

        } catch (Exception e) {
            return "";
        }
    }

    private void addHero(String title, String excerpt, String link, String imageUrl) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(round("#151515", 24));

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, dp(8), 0, dp(14));
        list.addView(card, cp);

        if (!TextUtils.isEmpty(imageUrl)) {
            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setBackgroundColor(Color.rgb(220, 220, 220));
            card.addView(img, new LinearLayout.LayoutParams(-1, dp(210)));
            new ImageTask(img).execute(imageUrl);
        }

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(14), dp(16), dp(16));
        card.addView(body);

        TextView badge = button("ÖNE ÇIKAN", THEME_COLOR, Color.WHITE);
        body.addView(badge, new LinearLayout.LayoutParams(dp(120), dp(34)));

        TextView t = text(title, 24, Color.WHITE, true);
        t.setPadding(0, dp(10), 0, dp(6));
        body.addView(t);

        TextView e = text(cut(excerpt, 140), 14, Color.rgb(220, 220, 220), false);
        body.addView(e);

        TextView open = button("HABERİ AÇ", THEME_COLOR, Color.WHITE);
        open.setOnClickListener(v -> openLink(link));

        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(-1, dp(48));
        op.setMargins(0, dp(12), 0, 0);
        body.addView(open, op);
    }

    private void addCard(String title, String excerpt, String link, String imageUrl) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(round("#ffffff", 20, "#eeeeee"));

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, 0, 0, dp(14));
        list.addView(card, cp);

        if (!TextUtils.isEmpty(imageUrl)) {
            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setBackgroundColor(Color.rgb(235, 235, 235));
            card.addView(img, new LinearLayout.LayoutParams(-1, dp(175)));
            new ImageTask(img).execute(imageUrl);
        }

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(14), dp(16), dp(16));
        card.addView(body);

        body.addView(text(title, 20, Color.rgb(20, 20, 20), true));

        TextView e = text(cut(excerpt, 135), 15, Color.rgb(95, 95, 95), false);
        e.setPadding(0, dp(8), 0, dp(12));
        body.addView(e);

        TextView open = button("HABERİ AÇ", "#eeeeee", Color.rgb(20, 20, 20));
        open.setOnClickListener(v -> openLink(link));
        body.addView(open, new LinearLayout.LayoutParams(-1, dp(44)));
    }

    private void openLink(String link) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
        } catch (Exception ignored) {
        }
    }

    private String clean(String html) {
        if (html == null) {
            return "";
        }

        return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
                .toString()
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String cut(String s, int len) {
        if (s == null) {
            return "";
        }

        return s.length() > len ? s.substring(0, len) + "..." : s;
    }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);

        if (bold) {
            t.setTypeface(Typeface.DEFAULT_BOLD);
        }

        return t;
    }

    private TextView button(String s, String bg, int color) {
        TextView b = new TextView(this);
        b.setText(s);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER);
        b.setTextColor(color);
        b.setBackground(round(bg, 12));
        return b;
    }

    private GradientDrawable round(String color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.parseColor(color));
        g.setCornerRadius(dp(radius));
        return g;
    }

    private GradientDrawable round(String color, int radius, String stroke) {
        GradientDrawable g = round(color, radius);
        g.setStroke(1, Color.parseColor(stroke));
        return g;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private class ImageTask extends AsyncTask<String, Void, Bitmap> {
        private final ImageView iv;

        ImageTask(ImageView iv) {
            this.iv = iv;
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            try {
                InputStream input = new URL(urls[0]).openStream();
                return BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                return null;
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
