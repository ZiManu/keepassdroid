/*
 * Copyright 2009 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Button;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.android.keepass.KeePass;
import com.android.keepass.R;
import com.keepassdroid.app.App;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.database.PwDatabaseV3;
import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwGroup;
import com.keepassdroid.database.PwGroupId;
import com.keepassdroid.database.PwGroupV3;
import com.keepassdroid.database.PwGroupV4;
import com.keepassdroid.database.edit.AddGroup;
import com.keepassdroid.view.ClickView;
import com.keepassdroid.view.GroupAddEntryView;
import com.keepassdroid.view.GroupRootView;
import com.keepassdroid.view.GroupViewOnlyView;

public abstract class GroupActivity extends GroupBaseActivity {
	
	public static final int UNINIT = -1;
	
	protected boolean addGroupEnabled = false;
	protected boolean addEntryEnabled = false;
	
	private static final String TAG = "Group Activity:";
	
	public static void Launch(Activity act) {
		Launch(act, null);
	}
	
	public static void Launch(Activity act, PwGroup group) {
		Intent i;
		
		// Need to use PwDatabase since group may be null
		PwDatabase db = App.getDB().pm;
		if ( db instanceof PwDatabaseV3 ) {
			i = new Intent(act, GroupActivityV3.class);
		
			if ( group != null ) {
				PwGroupV3 g = (PwGroupV3) group;
				i.putExtra(KEY_ENTRY, g.groupId);
			}
		} else if ( db instanceof PwDatabaseV4 ) {
			i = new Intent(act, GroupActivityV4.class);
			
			if ( group != null ) {
				PwGroupV4 g = (PwGroupV4) group;
				i.putExtra(KEY_ENTRY, g.uuid.toString());
			}
		} else {
			// Reached if db is null
			Log.d(TAG, "Tried to launch with null db");
			return;
		}
		
		act.startActivityForResult(i,0);
	}
	
	protected abstract PwGroupId retrieveGroupId(Intent i);
	
	protected abstract void setupButtons();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if ( isFinishing() ) {
			return;
		}
		
		setResult(KeePass.EXIT_NORMAL);
		
		Log.w(TAG, "Creating group view");
		Intent intent = getIntent();
		
		PwGroupId id = retrieveGroupId(intent);
		
		Database db = App.getDB();
		if ( id == null ) {
			mGroup = db.root;
		} else {
			mGroup = db.groups.get(id);
		}
		
		Log.w(TAG, "Retrieved group");
		if ( mGroup == null ) {
			Log.w(TAG, "Group was null");
			return;
		}
		
		setupButtons();

		if ( addGroupEnabled && addEntryEnabled ) {
			setContentView(new GroupAddEntryView(this));
		} else if ( addGroupEnabled ) {
			setContentView(new GroupRootView(this));
		} else if ( addEntryEnabled ) {
			throw new RuntimeException("This mode is not supported.");
		} else {
			setContentView(new GroupViewOnlyView(this));
		}
		Log.w(TAG, "Set view");

		if ( addGroupEnabled ) {
			// Add Group button
			Button addGroup = (Button) findViewById(R.id.add_group);
			addGroup.setOnClickListener(new GroupAddHandler(this, mGroup));
		}
		
		if ( addEntryEnabled ) {
			// Add Entry button
			Button addEntry = (Button) findViewById(R.id.add_entry);
			addEntry.setOnClickListener(new View.OnClickListener() {
	
				@Override
				public void onClick(View v) {
					EntryEditActivity.Launch(GroupActivity.this, mGroup);
				}
			});
		}
		
		setGroupTitle();

		setListAdapter(new PwListAdapter(this, mGroup));
		registerForContextMenu(getListView());
		Log.w(TAG, "Finished creating group");

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		
		AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) menuInfo;
		ClickView cv = (ClickView) acmi.targetView;
		cv.onCreateMenu(menu, menuInfo);
	}
	
	
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) item.getMenuInfo();
		ClickView cv = (ClickView) acmi.targetView;
		
		return cv.onContextItemSelected(item);
	}



	private class GroupAddHandler implements View.OnClickListener {
		private GroupBaseActivity mAct;
		private PwGroup mGroup;
		private GroupCreateDialog mDialog;
		
		GroupAddHandler(GroupBaseActivity act, PwGroup group) {
			mAct = act;
			mGroup = group;
		}

		@Override
		public void onClick(View v) {
			GroupCreateDialog dialog = new GroupCreateDialog(mAct);
			mDialog = dialog;
			
			// Register Listener
			dialog.setOnDismissListener(new Dialog.OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					String res = mDialog.getResponse();
					
					if ( ! mDialog.canceled() && res.length() > 0 ) {
						GroupActivity act = GroupActivity.this;
						Handler handler = new Handler();
						AddGroup task = AddGroup.getInstance(App.getDB(), res, mGroup, act.new RefreshTask(handler), false);
						ProgressTask pt = new ProgressTask(act, task, R.string.saving_database);
						pt.run();
					}
				}
				
			});
			
			// Show the dialog
			dialog.show();
		}
		
	}

}
