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
package com.keepassdroid.database.edit;

import java.io.IOException;

import com.keepassdroid.Database;
import com.keepassdroid.database.exception.PwDbOutputException;

public class SaveDB extends RunnableOnFinish {
	private Database mDb;
	private boolean mDontSave;

	public SaveDB(Database db, OnFinish finish, boolean dontSave) {
		super(finish);
		
		mDb = db;
		mDontSave = dontSave;
	}

	public SaveDB(Database db, OnFinish finish) {
		super(finish);
		
		mDb = db;
		mDontSave = false;
	}

	@Override
	public void run() {

		if ( ! mDontSave ) {
			try {
				mDb.SaveData();
			} catch (IOException e) {
				finish(false, e.getMessage());
				return;
			} catch (PwDbOutputException e) {
				finish(false, e.getMessage());
				return;
			}
		}

		finish(true);
	}

}
