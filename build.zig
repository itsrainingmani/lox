const std = @import("std");
const fs = std.fs;

pub fn build(b: *std.build.Builder) !void {
    const target = b.standardTargetOptions(.{});
    const mode = b.standardReleaseOptions();

    const flags = [_][]const u8{
        "-Wall",
        "-Wextra",
        "-Werror",
        "-Wno-unused-parameter",
    };

    const cflags = flags ++ [_][]const u8{"-std=c99"};

    var sources = std.ArrayList([]const u8).init(b.allocator);
    {
        var dir = try fs.cwd().openIterableDir("c", .{});

        var walker = try dir.walk(b.allocator);
        defer walker.deinit();

        const allowed_exts = [_][]const u8{
            ".c",
        };
        while (try walker.next()) |entry| {
            const ext = fs.path.extension(entry.basename);
            const include_file = for (allowed_exts) |e| {
                if (std.mem.eql(u8, ext, e))
                    break true;
            } else false;
            if (include_file) {
                // clone path or else walker.deinit() will dump the memory
                std.debug.print("Adding - {s: <10} ", .{entry.path});
                std.debug.print("{s: <5}\n", .{&cflags});
                try sources.append(b.pathJoin(&.{ "c", entry.path }));
            }
        }
    }

    const exe = b.addExecutable("clox", null);
    exe.setTarget(target);
    exe.setBuildMode(mode);

    exe.addIncludePath("c");
    exe.addCSourceFiles(sources.items, &cflags);

    exe.linkLibC();
    exe.install();

    const run_step = exe.run();

    // if we need to run a lox file
    if (b.args) |args| {
        run_step.addArgs(args);
    }
    const step = b.step("run", "Run the Clox Interpreter");
    step.dependOn(&run_step.step);
}
