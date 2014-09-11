package org.javastack.eraser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.SecureRandom;
import java.util.Arrays;

public class Eraser {
	public static final String PROP_ERASE_TYPE = "eraser.type";
	public static final String PROP_BLOCK_SIZE = "eraser.blocksize";
	public static final int DEFAULT_BLOCK_SIZE = 4096;
	public static final char MODE_ZERO = 'Z';
	public static final char MODE_ONE = 'O';
	public static final char MODE_RAND = 'R';

	private final SecureRandom rnd = new SecureRandom();
	private final byte[] buf;

	public Eraser() {
		this(DEFAULT_BLOCK_SIZE);
	}

	public Eraser(final int blockSize) {
		buf = new byte[blockSize];
	}

	public void erase(final File file, final char eraseType, final boolean showProgress)
			throws FileNotFoundException, IOException {
		if (!file.exists()) {
			throw new FileNotFoundException("File not found: " + file);
		}
		final long size = getSize(file);
		if (size <= 0) {
			throw new IOException("Size must be > 0: " + size);
		}
		fillBuffer(eraseType, buf);
		if (showProgress) {
			System.out.println(new StringBuilder(128).append("Wipe file=").append(file).append(" size=")
					.append(size).append(" blocksize=").append(DEFAULT_BLOCK_SIZE).append(" data=")
					.append(humanType(eraseType)).toString());
		}
		final RandomAccessFile raf = new RandomAccessFile(file, "rw");
		final long begin = System.currentTimeMillis();
		try {
			long remain = size;
			int dataWrite = 0, cntOutput = 0;
			raf.seek(0);
			while (remain > 0) {
				final int len = (int) Math.min((long) DEFAULT_BLOCK_SIZE, remain);
				if (showProgress) {
					if (dataWrite >= 0x100000) { // 1 MB
						System.out.print("w");
						dataWrite = 0;
						if (++cntOutput >= 64) { // 64 cols
							if (showProgress) {
								final long left = (remain * 100 / size);
								System.out.println(" (" + left + "% left)");
							}
							cntOutput = 0;
						}
					}
					dataWrite += len;
				}
				raf.write(buf, 0, len);
				remain -= len;
			}
		} finally {
			final long endUnsync = System.currentTimeMillis();
			if (raf != null) {
				try {
					if (showProgress) {
						System.out.println("s");
					}
					raf.getFD().sync();
				} catch (Exception ign) {
				}
			}
			if (showProgress) {
				final long endSync = System.currentTimeMillis();
				final float timeSync = (Math.max(endSync - endUnsync, 1) / 1000f);
				final float time = (Math.max(endSync - begin, 1) / 1000f);
				final float throughput = (((float) size) / Math.max(time, 1f));
				System.out.println(new StringBuilder(128).append(size).append(" bytes ")
						.append(humanSize(size)).append(", ").append(time).append(" s (sync ")
						.append(timeSync).append(" s), ").append(humanThroughput(throughput)).toString());
			}
			if (raf != null) {
				try {
					raf.close();
				} catch (Exception ign) {
				}
			}
		}
	}

	private final void fillBuffer(final char type, final byte[] buffer) {
		switch (type) {
			case MODE_ZERO:
				Arrays.fill(buffer, (byte) 0);
				break;
			case MODE_ONE:
				Arrays.fill(buffer, (byte) 0xFF);
				break;
			case MODE_RAND:
				rnd.nextBytes(buffer);
				break;
			default:
				throw new IllegalArgumentException("Invalid type");
		}
	}

	private static final long getSize(final File file) throws IOException {
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(file, "r");
			return raf.length();
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (Exception ign) {
				}
			}
		}
	}

	private static final String humanSize(final long size) {
		float r = size;
		int exp = 0;
		while (r > 1000) {
			r /= 1000;
			exp += 3;
		}
		final String unit = mapUnitBase10(exp);
		if (unit == null)
			return null;
		return "(" + Math.round(r) + " " + unit + ")";
	}

	private static final String humanThroughput(final float throughput) {
		float r = throughput;
		int exp = 0;
		while (r > 1024) {
			r /= 1024;
			exp += 10;
		}
		final String unit = mapUnitBase2(exp);
		if (unit == null)
			return null;
		return Math.round(r) + " " + unit + "/s";
	}

	private static final String humanType(final char type) {
		switch (type) {
			case MODE_ZERO:
				return "ALL-ZERO";
			case MODE_ONE:
				return "ALL-ONE";
			case MODE_RAND:
				return "RANDOM";
			default:
				throw new IllegalArgumentException("Invalid type");
		}
	}

	private static final String mapUnitBase2(final int exp) {
		switch (exp) {
			case 0:
				return "B";
			case 10:
				return "KiB";
			case 20:
				return "MiB";
			case 30:
				return "GiB";
			case 40:
				return "TiB";
			case 50:
				return "PiB";
			case 60:
				return "EiB";
			case 70:
				return "ZiB";
			case 80:
				return "YiB";
		}
		return null;
	}

	private static final String mapUnitBase10(final int exp) {
		switch (exp) {
			case 0:
				return "B";
			case 3:
				return "kB";
			case 6:
				return "MB";
			case 9:
				return "GB";
			case 12:
				return "TB";
			case 15:
				return "PB";
			case 18:
				return "EB";
			case 21:
				return "ZB";
			case 24:
				return "YB";
		}
		return null;
	}

	public static void main(final String[] args) throws Throwable {
		if (args.length < 1) {
			System.err.println("java " + Eraser.class.getName() + " <file> [<file> [...]]");
			return;
		}
		final int blockSize = Integer.getInteger(PROP_BLOCK_SIZE, DEFAULT_BLOCK_SIZE).intValue();
		final char[] eraseTypes = System.getProperty(PROP_ERASE_TYPE, "OZR").toUpperCase().toCharArray();
		for (final String a : args) {
			final File f = new File(a);
			if (!f.exists()) {
				System.err.println("ERROR: File not found: " + f);
				continue;
			}
			final Eraser e = new Eraser(blockSize);
			for (final char eraseType : eraseTypes) {
				e.erase(f, eraseType, true);
			}
		}
	}
}
