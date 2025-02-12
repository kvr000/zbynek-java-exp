package com.github.kvr000.exp.java.video.ffmpeg.command;

import com.github.kvr000.exp.java.video.ffmpeg.FfmpegExp;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.dryuf.cmdline.command.AbstractCommand;
import net.dryuf.cmdline.command.CommandContext;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@RequiredArgsConstructor(onConstructor = @__(@Inject))
@Log4j2
public class H265EncodeCommand extends AbstractCommand
{
	private final FfmpegExp.MainOptions mainOptions;

	private final Options options = new Options();

	@Override
	protected boolean parseOption(CommandContext context, String arg, ListIterator<String> args) throws Exception
	{
		switch (arg) {
		case "--ve":
			options.videoEncoder = needArgsParam(options.videoEncoder, args);
			return true;

		case "--bitrate-ratio":
			options.bitrateRatio = Integer.valueOf(needArgsParam(options.bitrateRatio, args));
			return true;

		case "--start-time":
			options.startTime = Double.valueOf(needArgsParam(options.startTime, args));
			return true;

		case "--end-time":
			options.endTime = Double.valueOf(needArgsParam(options.endTime, args));
			return true;

		default:
			return super.parseOption(context, arg, args);
		}
	}

	@Override
	protected int validateOptions(CommandContext context, ListIterator<String> args) throws Exception
	{
		if (options.bitrateRatio == null) {
			options.bitrateRatio = 100;
		}
		return EXIT_CONTINUE;
	}

	@Override
	protected int parseNonOptions(CommandContext context, ListIterator<String> args) throws Exception
	{
		options.input = needArgsParam(options.input, args);
		return EXIT_CONTINUE;
	}

	@Override
	protected Map<String, String> configOptionsDescription(CommandContext context)
	{
		return ImmutableMap.of(
			"--ve encoder", "video encoder (such as h264_qsv)",
			"--bitrate-ratio ratio-percent", "drop ratio to ratio-percent (1-100)",
			"--start-time time", "start at this time",
			"--end-time time", "cut video after time (exclusive)"
		);
	}

	@Override
	public int execute() throws Exception
	{
		FFmpegLogCallback.set();
		String ffmpegVersion = avutil.av_version_info().getString(StandardCharsets.UTF_8);
		log.info("FFmpeg Version: {}", ffmpegVersion);
		log.info("FFmpeg Build Configuration: {}", avutil.avutil_configuration().getString(StandardCharsets.UTF_8));
		log.info("Codecs:");
		{
			// Initialize the codec iterator
			BytePointer opaque = new BytePointer();

			// Iterate over all codecs
			AVCodec codec;
			while ((codec = avcodec.av_codec_iterate(opaque)) != null) {
				//opaque = new BytePointer(opaque); // Update the iterator

				// Check if it's a video or audio codec
				// Print codec details
				log.info("\tCodec Name: {}", codec.name().getString());
			}
		}

		Stopwatch stopwatch = Stopwatch.createUnstarted();
		try (
			FFmpegFrameGrabber grabber = openGrabber();
			FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(mainOptions.getVideoOutput(), grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());
			Java2DFrameConverter converter = new Java2DFrameConverter()
		) {
			log.info("videoMetadata: {}", grabber.getVideoMetadata());
			log.info("audioMetadata: {}", grabber.getAudioMetadata());
			grabber.setOption("threads", String.valueOf(Runtime.getRuntime().availableProcessors()));  // Use automatic thread detection

			int numStreams = grabber.getLengthInFrames();
			log.info("Number of frames: " + numStreams);

			recorder.setFormat("mp4");
			recorder.setOption("loglevel", "debug");
			recorder.setOption("threads", "auto");
			recorder.setOption("c", "copy");
			recorder.setFrameRate(grabber.getFrameRate());
			if (grabber.hasVideo()) {
				recorder.setVideoMetadata(grabber.getVideoMetadata());
				recorder.setVideoCodec(avcodec.AV_CODEC_ID_H265);
				recorder.setVideoCodecName("libx265");
				recorder.setVideoOption("tag:v", "hvc1");
				recorder.setOption("tag:v", "hvc1");

				recorder.setVideoBitrate(grabber.getVideoBitrate() * options.bitrateRatio / 100);
				// those do not work:
				recorder.setOption("threads", "auto");
				recorder.setOption("frame-threads", "4");
				recorder.setVideoOption("threads", "auto");
				recorder.setVideoOption("frame-threads", "4");
				//recorder.setVideoOption("preset", "ultrafast");
				//recorder.setVideoOption("tune", "film"); // unsupported
				//recorder.setVideoOption("crf", "23");
				recorder.setVideoOption("preset", "slow");
			}
			if (grabber.hasAudio()) {
				recorder.setAudioChannels(grabber.getAudioChannels());
				recorder.setAudioMetadata(grabber.getAudioMetadata());
				recorder.setAudioCodec(grabber.getAudioCodec());
				recorder.setAudioBitrate(grabber.getAudioBitrate());
			}

			recorder.start();

			//log.info("threads: {}", video_c.thread_count());

			if (options.startTime != null) {
				grabber.setTimestamp((long) (options.startTime * 1_000_000));
			}

			stopwatch.start();

			// Frame processing loop
			Frame frame;
			while ((frame = grabber.grab()) != null) {
				recorder.record(frame);
			}

			grabber.stop();
			recorder.stop();
		}
		finally {
			log.info("Finished in: {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
		}

		return 0;
	}

	private FFmpegFrameGrabber openGrabber() throws FrameGrabber.Exception
	{
		FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(options.input);
		try {
			grabber.start();
		}
		catch (Throwable ex) {
			grabber.close();
			throw ex;
		}
		return grabber;
	}

	public static class Options
	{
		String input;

		String videoCodec;

		String audioCodec;

		String videoEncoder;

		Double startTime;

		Double endTime;

		Integer bitrateRatio;
	}
}
