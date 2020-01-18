package net.jk.app.videostreamer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;

import lombok.Builder;
import lombok.Data;

@Controller(value = "/")
public class PageController {

	@Value("${video.location}")
	private String videoLocation;

	private static final Escaper ESCAPER = UrlEscapers.urlPathSegmentEscaper();

	private static final List<MediaType> MEDIA_TYPES = new ArrayList<>();

	@PostConstruct
	private void init() {
		MEDIA_TYPES.add(MediaType.valueOf("video/x-matroska"));
		MEDIA_TYPES.add(MediaType.valueOf("video/x-msvideo"));
		MEDIA_TYPES.add(MediaType.valueOf("video/x-ms-wmv"));
		MEDIA_TYPES.add(MediaType.valueOf("video/mp4"));
		MEDIA_TYPES.add(MediaType.valueOf("video/x-ms-vob"));
	}

	@GetMapping("/")
	public String index(Model model) throws IOException {
		// getting all of the files in video folder
		List<Video> videos = Files.walk(Paths.get(videoLocation)).filter(Files::isRegularFile).filter(this.isVideoFile)
				.map(this.videoMapper).collect(Collectors.toList());
		model.addAttribute("videos", videos);
		return "index";
	}

	@GetMapping("/{videoName}")
	public String video(@PathVariable String videoName, Model model) throws UnsupportedEncodingException {
		model.addAttribute("videoName", ESCAPER.escape(videoName));
		return "video";
	}

	public Predicate<Path> isVideoFile = (p) -> {
		MediaType mediaType = MediaTypeFactory.getMediaType(p.getFileName().toString()).orElse(null);
		return MEDIA_TYPES.contains(mediaType);
	};

	public Function<Path, Video> videoMapper = (p) -> {
		return Video.builder().fileName(p.getFileName().toString())
				.location(ESCAPER.escape(p.toAbsolutePath().toString())).build();
	};

	@Data
	@Builder
	public static class Video {
		private String fileName;
		private String location;
	}
}
