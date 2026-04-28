package com.lakshy.blog.services.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.lakshy.blog.exceptions.ApiException;
import com.lakshy.blog.exceptions.IncorrectFileFormatException;
import com.lakshy.blog.exceptions.ResourceNotFoundException;
import com.lakshy.blog.services.FileService;

@Service
public class FileServiceImpl implements FileService {

	@Override
	public String uploadImage(String path, MultipartFile file) throws IOException {
		String originalFilename = file.getOriginalFilename();
		if (originalFilename == null || originalFilename.isBlank()) {
			throw new IncorrectFileFormatException("empty filename");
		}
		// Use FilenameUtils to safely extract only the extension, stripping any path components
		String fileExtension = "." + FilenameUtils.getExtension(originalFilename).toLowerCase();
		if (!(fileExtension.equals(".png") || fileExtension.equals(".jpg") || fileExtension.equals(".jpeg"))) {
			throw new IncorrectFileFormatException(fileExtension);
		}

		// Random name generator file
		String randomId = UUID.randomUUID().toString();
		String modifiedFileName = randomId + fileExtension;
		
		// Full path
		// File.Separator added / and \ slash depending upon OS
		String filePath = path + File.separator + modifiedFileName;
		
		//create folder if not created
		File f = new File(path);
		if(!f.exists()) {
			f.mkdir();
		}
		
		Files.copy(file.getInputStream(), Paths.get(filePath));
		
		return modifiedFileName;
	}

	@Override
	public InputStream getResources(String path, String fileName) throws FileNotFoundException {
		// Sanitise: reject any traversal sequences before touching the filesystem.
		if (fileName == null || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
			throw new ApiException("Invalid file name");
		}
		// Secondary check: resolve and verify the canonical path stays inside the upload directory.
		Path uploadDir = Paths.get(path).toAbsolutePath().normalize();
		Path resolved = uploadDir.resolve(fileName).normalize();
		if (!resolved.startsWith(uploadDir)) {
			throw new ApiException("Invalid file name");
		}

		String fullPath = resolved.toString();
		InputStream is = new FileInputStream(fullPath);
		return is;
	}

}
