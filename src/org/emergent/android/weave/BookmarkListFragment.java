/*
 * Copyright 2012 Patrick Woodworth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.emergent.android.weave;

import org.emergent.android.weave.persistence.Bookmarks;
import org.emergent.android.weave.util.Dbg.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * @author Patrick Woodworth
 */
public class BookmarkListFragment extends WeaveListFragment {

  public static final String PARENT_UUID_BUNDLE_KEY = "parentUuid";
  public static final String PARENT_NAME_BUNDLE_KEY = "parentName";

  private String m_parentUuid = null;
  private String m_parentName = null;

  @Override
  public void onInnerActivityCreated(Bundle savedInstanceState) {
    super.onInnerActivityCreated(savedInstanceState);
  }

  @Override
  public void onInnerPostAdapterSetup(Bundle savedInstanceState) {
    super.onInnerPostAdapterSetup(savedInstanceState);
    Bundle stateBundle = savedInstanceState;
    if (stateBundle == null) {
      Activity activity = getActivity();
      if (activity instanceof FragUtils.FragmentDataStore) {
        stateBundle = ((FragUtils.FragmentDataStore)activity).getFragData(getFragTag());
      }
    }

    Bundle args = getArguments();
    if (args != null && args.containsKey(PARENT_UUID_BUNDLE_KEY)) {
      String parentUuid = args.getString(PARENT_UUID_BUNDLE_KEY);
      if (!StaticUtils.isEmpty(parentUuid)) {
        m_parentUuid = parentUuid;
        validateFilter();
      }
    }

    if (stateBundle == null) {
      return;
    }
  }

  @Override
  public void onInnerSaveInstanceState(Bundle outState) {
    super.onInnerSaveInstanceState(outState);
    outState.putString(PARENT_UUID_BUNDLE_KEY, m_parentUuid);
    outState.putString(PARENT_NAME_BUNDLE_KEY, m_parentName);
  }

  public String getParentFolderUuid() {
    String retval = m_parentUuid;
    if (retval == null)
      retval = "places";
    return retval;
  }

  protected void moveIntoFolder(String uuid, String title) {
    MainActivity activity = (MainActivity)getActivity();
    Bundle args = new Bundle();
    args.putString(PARENT_UUID_BUNDLE_KEY, uuid);
    args.putString(PARENT_NAME_BUNDLE_KEY, title);
    BookmarkListFragment newfrag = new BookmarkListFragment();
    newfrag.setArguments(args);
    activity.setMyFragment(newfrag);
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    Activity activity = getActivity();
    if (activity == null) {
      Log.w(TAG, "onListItemClick: Activity was null!");
      return;
    }

    ListAdapter listAdapter = getListAdapter();
    Cursor cursor = (Cursor)listAdapter.getItem(position);

    String type = cursor.getString(cursor.getColumnIndex(Bookmarks.Columns.TYPE));
    if ("folder".equals(type)) {
      String uuid = cursor.getString(cursor.getColumnIndex(Bookmarks.Columns.UUID));
      String title = cursor.getString(cursor.getColumnIndex(Bookmarks.Columns.TITLE));
      Log.d(TAG, String.format("BookmarkListFragment.onListItemClick: %s \"%s\" %s", type, title, uuid));
      moveIntoFolder(uuid, title);
    } else {
      String uri = cursor.getString(cursor.getColumnIndex(Bookmarks.Columns.BMK_URI));
      Log.d(TAG, String.format("BookmarkListFragment.onListItemClick: %s \"%s\"", type, uri));
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
      startActivity(intent);
    }
  }

  @Override
  protected Bundle createLoaderBundle() {
    Bundle args = new Bundle();
    args.putCharSequence(QUERY_KEY, m_filterString.get());
    args.putParcelable(CONTENT_URI_KEY, Bookmarks.CONTENT_URI);
    args.putString(SORT_ORDER_KEY, Bookmarks.Columns.TITLE);
    return args;
  }

  @Override
  protected WeaveLoaderCallbacks createLoaderCallbacks() {
    return new BookmarkLoaderCallbacks(this);
  }

  @Override
  protected SimpleCursorAdapter createWeaveCursorAdapter() {
    Context activity = getCursorLoaderContext();
    String[] from = {Bookmarks.Columns.TITLE, Bookmarks.Columns.BMK_URI};
    int[] to = {R.id.title, R.id.url};
    return new WeaveListCursorAdapter(activity, R.layout.history_item, from, to);
  }

  public static class BookmarkLoaderCallbacks extends WeaveLoaderCallbacks {

    private final BookmarkListFragment m_activity;

    public BookmarkLoaderCallbacks(BookmarkListFragment activity) {
      super(activity);
      m_activity = activity;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
      if (args == null)
        throw new NullPointerException("BookmarkLoaderCallbacks.onCreateLoader: args were null!");

      CharSequence charSequence = args.getCharSequence(QUERY_KEY);
      Uri contentUri = args.getParcelable(CONTENT_URI_KEY);
      String sortOrder = args.getString(SORT_ORDER_KEY);
      String selection;
      String[] selectionArgs;
      if (TextUtils.isEmpty(charSequence)) {
        selection = "" + Bookmarks.Columns.IS_DELETED + " = ?";
        selection += " AND " + Bookmarks.Columns.TYPE + " NOT IN (?, ?, ?)";
        selection += " AND ( " + Bookmarks.Columns.TITLE + " != ? OR " + Bookmarks.Columns.BMK_URI + " != ? )";
        selection += " AND " + Bookmarks.Columns.PARENT_ID + " = ?";
        String parentId = m_activity.getParentFolderUuid();
        selectionArgs = new String[]{"0", "livemark", "query", "separator", "", "", parentId};
      } else {
        selection = "(" + Bookmarks.Columns.TYPE + " = ?)";
        selection += " AND ((" + Bookmarks.Columns.TITLE + " LIKE ?)";
        selection += " OR (" + Bookmarks.Columns.BMK_URI + " LIKE ?)";
        selection += " OR (" + Bookmarks.Columns.TAGS + " LIKE ?))";
        selectionArgs = new String[]{
            "bookmark",
            "%" + charSequence.toString() + "%",
            "%" + charSequence.toString() + "%",
            "%|" + charSequence.toString() + "|%"
        };
      }
      return new CursorLoader(m_activity.getCursorLoaderContext(), contentUri, null, selection, selectionArgs, sortOrder);
    }
  }
}
