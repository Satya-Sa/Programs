package com.demo.controller;

import static java.nio.file.Files.write;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.demo.data.FileUploadRepository;
import com.demo.exception.ApplicationError;
import com.demo.model.FileMetaData;

/**
 * A rest controller provides api to upload single/multiple files as post
 * request and get the uploaded files as an get request
 */
@RestController
public class FileUploadController {
	private final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

	/** Save the uploaded file to this folder */
	@Value("${file.upload.folder}")
	private String UPLOADED_FOLDER;

	/**
	 * This object is required to store file meta data into in memory database
	 */
	@Autowired
	private FileUploadRepository fileUploadMetaData;

	@GetMapping("/api/ping")
	public String index() {
		return "pinged me!";
	}

	/**
	 * Single file upload
	 * 
	 * @param uploadfile
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/api/uploadfile", method = RequestMethod.POST)
	public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile uploadfile,
			final HttpServletRequest request) {

		/** Below data is what we saving into database */
		logger.debug("Single file upload!");
		logger.debug("fileName : " + uploadfile.getOriginalFilename());
		logger.debug("contentType : " + uploadfile.getContentType());
		logger.debug("contentSize : " + uploadfile.getSize());

		if (uploadfile.isEmpty()) {
			return new ResponseEntity<String>("please select a file!", HttpStatus.OK);
		}

		try {
			/** File will get saved to file system and database */
			saveUploadedFiles(Arrays.asList(uploadfile));
		} catch (IOException e) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<String>("Successfully uploaded - " + uploadfile.getOriginalFilename(),
				new HttpHeaders(), HttpStatus.OK);

	}


	/**
	 * Rest endpoint api to get uploaded files
	 * 
	 * @return
	 */
	@RequestMapping(value = "/api/getAllFilesMetaData", method = RequestMethod.GET)
	public List<FileMetaData> fileUploadMetaData() {
		List<FileMetaData> fileMetaData = fileUploadMetaData.findAll();
		return fileMetaData;
	}

	/**
	 * Rest endpoint api to get uploaded files
	 * 
	 * @return
	 */
	@RequestMapping(value = "/api/getFile", method = RequestMethod.GET)
	public ResponseEntity<?> getFile(@RequestParam("metaId") Long metaId) {
		Optional<FileMetaData> fileMetaData = fileUploadMetaData.findById(metaId);
		if (fileMetaData.isEmpty()) {
			return new ResponseEntity<ApplicationError>(new ApplicationError("FNF", "File Not Found"), HttpStatus.OK);
		}
		Path path = Paths.get(UPLOADED_FOLDER + fileMetaData.get().getName());

		Resource resource = new PathResource(path);
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(fileMetaData.get().getContentType()))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
				.body(resource);
	}

	/**
	 * Files will get saved to file system and database
	 * 
	 * @param files
	 * @throws IOException
	 */
	private void saveUploadedFiles(List<MultipartFile> files) throws IOException {
		for (MultipartFile file : files) {
			if (file.isEmpty()) {
				continue;
			}
			byte[] bytes = file.getBytes();
			Path path = Paths.get(UPLOADED_FOLDER + file.getOriginalFilename());
			write(path, bytes);
			saveMetaData(file);

		}

	}

	/**
	 * Saves meta data to database
	 * 
	 * @param file
	 * @throws IOException
	 */
	private void saveMetaData(MultipartFile file) throws IOException {
		FileMetaData metaData = new FileMetaData();
		metaData.setName(file.getOriginalFilename());
		metaData.setContentType(file.getContentType());
		metaData.setContentSize(file.getSize());
		fileUploadMetaData.save(metaData);
	}
}
