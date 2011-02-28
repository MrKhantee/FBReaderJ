/*
 * Copyright (C) 2010-2011 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader.network;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.network.ZLNetworkException;

import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.fbreader.network.ICustomNetworkLink;
import org.geometerplus.fbreader.network.opds.OPDSLinkReader;

import org.geometerplus.android.util.UIUtil;

public class AddCustomCatalogActivity extends Activity {
	private ZLResource myResource;
	private String myURL;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Thread.setDefaultUncaughtExceptionHandler(new org.geometerplus.zlibrary.ui.android.library.UncaughtExceptionHandler(this));
		setContentView(R.layout.add_custom_catalog);

		myResource = ZLResource.resource("dialog").getResource("CustomCatalogDialog");

		setTitle(myResource.getResource("title").getValue());

		setTextFromResource(R.id.add_custom_catalog_title_label, "catalogTitle");
		setTextFromResource(R.id.add_custom_catalog_url_label, "catalogUrl");
		setTextFromResource(R.id.add_custom_catalog_summary_label, "catalogSummary");
		setTextFromResource(R.id.add_custom_catalog_title_example, "catalogTitleExample");
		setTextFromResource(R.id.add_custom_catalog_url_example, "catalogUrlExample");
		setTextFromResource(R.id.add_custom_catalog_summary_example, "catalogSummaryExample");

		setupButton(
			R.id.add_custom_catalog_ok_button, "ok", new View.OnClickListener() {
				public void onClick(View view) {
					if (isEmptyString(myURL)) {
						final String url = getTextById(R.id.add_custom_catalog_url);
						if (isEmptyString(url)) {
							setErrorByKey("urlIsEmpty");
						} else {
							try {
								Uri uri = Uri.parse(url);
								if (isEmptyString(uri.getScheme())) {
									uri = Uri.parse("http://" + url);
								}
								if (isEmptyString(uri.getHost())) {
									setErrorByKey("invalidUrl");
								} else {
									loadInfoByUri(uri);
								}
							} catch (Throwable t) {
								setErrorByKey("invalidUrl");
							}
						}
					} else {
						gotoNetworkLibrary();
					}
				}
			}
		);
		setupButton(
			R.id.add_custom_catalog_cancel_button, "cancel", new View.OnClickListener() {
				public void onClick(View view) {
					finish();
				}
			}
		);

		final Uri uri = getIntent().getData();
		if (uri != null) {
			loadInfoByUri(uri);
			setExtraFieldsVisibility(true);
		} else {
			myURL = null;
			setExtraFieldsVisibility(false);
		}
	}

	private boolean isEmptyString(String s) {
		return s == null || s.length() == 0;
	}

	private void setExtraFieldsVisibility(boolean show) {
		final int visibility = show ? View.VISIBLE : View.GONE;
		runOnUiThread(new Runnable() {
			public void run() {
				findViewById(R.id.add_custom_catalog_title_group).setVisibility(visibility);
				findViewById(R.id.add_custom_catalog_summary_group).setVisibility(visibility);
			}
		});
	}

	private void gotoNetworkLibrary() {
		startActivity(
			new Intent(AddCustomCatalogActivity.this, NetworkLibraryActivity.class)
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
		);
		finish();
	}

	private void setTextById(int id, String text) {
		((TextView)findViewById(id)).setText(text);
	}

	private String getTextById(int id) {
		final String text = ((TextView)findViewById(id)).getText().toString();
		return text != null ? text.trim() : null;
	}

	private void setupButton(int id, String resourceKey, View.OnClickListener listener) {
		final Button button = (Button)findViewById(id);
		button.setText(
			ZLResource.resource("dialog").getResource("button").getResource(resourceKey).getValue()
		);
		button.setOnClickListener(listener);
	}

	private void setTextFromResource(int id, String resourceKey) {
		setTextById(id, myResource.getResource(resourceKey).getValue());
	}

	private void setErrorText(final String errorText) {
		runOnUiThread(new Runnable() {
			public void run() {
				final TextView errorView = (TextView)findViewById(R.id.add_custom_catalog_error);
				if (errorText != null) {
					errorView.setText(errorText);
					errorView.setVisibility(View.VISIBLE);
				} else {
					errorView.setVisibility(View.GONE);
				}
			}
		});
	}

	private void setErrorByKey(final String resourceKey) {
		setErrorText(myResource.getResource(resourceKey).getValue());
	}

	private void runErrorDialog(final String errorText) {
		final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
					case DialogInterface.BUTTON_POSITIVE:
						setExtraFieldsVisibility(true);
						break;
					case DialogInterface.BUTTON_NEUTRAL:
						break;
					case DialogInterface.BUTTON_NEGATIVE:
						AddCustomCatalogActivity.this.finish();
						break;
				}
			}
		};

		final ZLResource dialogResource = ZLResource.resource("dialog");
		final ZLResource boxResource = dialogResource.getResource("networkError");
		final ZLResource buttonResource = dialogResource.getResource("button");
		new AlertDialog.Builder(this)
			.setTitle(boxResource.getResource("title").getValue())
			.setMessage(errorText)
			.setIcon(0)
			.setPositiveButton(buttonResource.getResource("continue").getValue(), listener)
			.setNeutralButton(buttonResource.getResource("editUrl").getValue(), listener)
			.setNegativeButton(buttonResource.getResource("cancel").getValue(), listener)
			.create().show();
	}

	private void loadInfoByUri(Uri uri) {
		myURL = uri.toString();
		if (isEmptyString(uri.getScheme())) {
			myURL = "http://" + myURL;
			uri = Uri.parse(myURL);
		} else if ("opds".equals(uri.getScheme())) {
			myURL = "http" + uri.toString().substring(4);
		}

		setTextById(R.id.add_custom_catalog_url, myURL);
		String siteName = uri.getHost();
		if (isEmptyString(siteName)) {
			setErrorByKey("invalidUrl");
			return;
		}

		if (siteName.startsWith("www.")) {
			siteName = siteName.substring(4);
		}
		final ICustomNetworkLink link =
		OPDSLinkReader.createCustomLinkWithoutInfo(siteName, myURL);

		final Runnable loadInfoRunnable = new Runnable() {
			private String myError;

			public void run() {
				try {
					myError = null;
					link.reloadInfo();
				} catch (ZLNetworkException e) {
					myError = e.getMessage();
				}
				runOnUiThread(new Runnable() {
					public void run() {
						if (myError == null) {
							setTextById(R.id.add_custom_catalog_title, link.getTitle());
							setTextById(R.id.add_custom_catalog_summary, link.getSummary());
							setExtraFieldsVisibility(true);
						} else {
							runErrorDialog(myError);
						}
					}
				});
				setErrorText(myError);
			}
		}; 
		UIUtil.wait("loadingCatalogInfo", loadInfoRunnable, this);
	}
}
