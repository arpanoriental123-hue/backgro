package com.lakshy.blog.config;

import java.util.Set;

// All constants and hard coded values
public class AppConstants {
	public static final String PAGE_NUMBER = "0";
	public static final String PAGE_SIZE = "10";
	public static final String SORT_BY = "postId";
	public static final String SORT_DIR = "asc";
	public static final Integer NORMAL_USER = 502;
	public static final Integer ADMIN_USER = 501;
	public static final Set<String> ALLOWED_SORT_FIELDS = Set.of("postId", "title", "addedDate");
	/** Allowed values for the sortDir query parameter. */
	public static final Set<String> ALLOWED_SORT_DIRS = Set.of("asc", "desc");
}
