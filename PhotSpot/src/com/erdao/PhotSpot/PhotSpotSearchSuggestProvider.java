/*
 * Copyright (C) 2009 Huan Erdao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.erdao.PhotSpot;

import android.content.SearchRecentSuggestionsProvider;

/**
 * Custom SearchRecentSuggestionsProvider to store suggestion
 * @author Huan Erdao
 */
public class PhotSpotSearchSuggestProvider extends SearchRecentSuggestionsProvider {
	/** AUTHORITY */
	final static String AUTHORITY = "com.erdao.PhotSpot.PhotSpotSearchSuggestProvider";
	/** MODE */
	final static int MODE = DATABASE_MODE_QUERIES;
	public PhotSpotSearchSuggestProvider() {
		super();
		setupSuggestions(AUTHORITY, MODE);
	}
}
