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
public class CopyCommand extends AbstractCommand
{
	private final FfmpegExp.MainOptions mainOptions;

	private final Options options = new Options();

	@Override
	protected boolean parseOption(CommandContext context, String arg, ListIterator<String> args) throws Exception
	{
		switch (arg) {
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
				recorder.setVideoCodec(grabber.getVideoCodec());
				recorder.setVideoCodecName("copy");

				recorder.setVideoBitrate(grabber.getVideoBitrate());
			}
			if (grabber.hasAudio()) {
				recorder.setAudioChannels(grabber.getAudioChannels());
				recorder.setAudioMetadata(grabber.getAudioMetadata());
			}

			recorder.start();

			if (options.startTime != null) {
				grabber.setTimestamp((long) (options.startTime * 1_000_000));
			}

			stopwatch.start();

			// Frame processing loop
			Frame frame;
			while ((frame = grabber.grab()) != null) {
				if (frame.type == Frame.Type.VIDEO) {
					if (options.endTime != null && frame.timestamp >= Math.ceil(options.endTime * 1_000_000)) {
						break;
					}
				}
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

		Double startTime;

		Double endTime;
	}
}
