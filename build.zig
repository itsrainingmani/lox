const std = @import("std");
const fs = std.fs;

pub fn build(b: *std.Build) !void {
    const target = b.standardTargetOptions(.{});
    const optimize = b.standardOptimizeOption(.{});

    const flags = [_][]const u8{
        "-Wall",
        "-Wextra",
        "-Werror",
        "-Wno-unused-parameter",
    };

    const cflags = flags ++ [_][]const u8{"-std=c99"};

    std.debug.print("Walking source dir and adding c files\n\n", .{});
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
                std.debug.print("{s: <10} {s: <5}\n", .{ entry.path, &cflags });
                // std.debug.print("\n", .{&cflags});
                try sources.append(b.pathJoin(&.{ "c", entry.path }));
            }
        }
    }

    std.debug.print("Building clox executable\n\n", .{});

    const exe = b.addExecutable(.{ .name = "clox", .target = target, .optimize = optimize });

    exe.addIncludePath(.{ .path = "c" });
    exe.addCSourceFiles(sources.items, &cflags);

    exe.linkLibC();
    b.installArtifact(exe);

    const repl = b.addRunArtifact(exe);
    const run = b.addRunArtifact(exe);

    // if we need to run a lox file
    if (b.args) |args| {
        run.addArgs(args);
    }
    const repl_step = b.step("repl", "Open the Clox REPL");
    const run_step = b.step("run", "Run the file through Clox");

    repl_step.dependOn(&repl.step);
    run_step.dependOn(&run.step);
}
