const std = @import("std");
const fs = std.fs;

const Allocator = std.heap.ArenaAllocator;
const Dir = std.fs.Dir;
const IterableDir = std.fs.IterableDir;
const File = std.fs.File;

pub fn build(b: *std.build.Builder) !void {
	const target = b.standardTargetOptions(.{});
	const mode = b.standardReleaseOptions();

	const flags = [_][]const u8 {
		"-Wall",
		"-Wextra",
		"-Werror",
		"-Wno-unused-parameter",
	};

	const cflags = flags ++ [_][]const u8 {
		"-std=c99"
	};

	var sources = std.ArrayList([]const u8).init(b.allocator);
	// var arena = Allocator.init(std.heap.page_allocator);
	// defer arena.deinit();
	// const alloc = arena.allocator();

	{
		var dir = try fs.cwd().openIterableDir("c", .{});

		var walker = try dir.walk(b.allocator);
		defer walker.deinit();

		const allowed_exts = [_][]const u8{ ".c",};
		while (try walker.next()) |entry| {
			const ext = fs.path.extension(entry.basename);
			const include_file = for (allowed_exts) |e| {
				if (std.mem.eql(u8, ext, e))
					break true;
			} else false;
			if (include_file) {
				// clone path or else walker.deinit() will dump the memory
				std.debug.print("Adding - {s}\n", .{entry.path});
				try sources.append(b.pathJoin(&.{"c", entry.path}));
			}
		}
	}

	const exe = b.addExecutable("lox", null);
	exe.setTarget(target);
	exe.setBuildMode(mode);

	exe.addIncludePath("c");
	exe.addCSourceFiles(sources.items, &cflags);

	exe.linkLibC();
	exe.install();
}