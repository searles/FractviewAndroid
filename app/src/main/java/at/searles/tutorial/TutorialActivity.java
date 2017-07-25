package at.searles.tutorial;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import at.searles.fractview.R;

public class TutorialActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        webView = (WebView) findViewById(R.id.webView);
        webView.loadUrl("file:///android_asset/tutorial/quickstart.html");


        webView.setWebViewClient(new WebViewClient(){
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (uri != null && (uri.getScheme().equals("http:") || uri.equals("https:"))) {
                    view.getContext().startActivity(
                            new Intent(Intent.ACTION_VIEW, uri));
                    return true;
                } else {
                    return false;
                }
            }
        });


        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                // FIXME Load http-stuff in browser.
                return false;
            }
        });

        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
