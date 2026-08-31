# Plugin Hub submission checklist

This repository is prepared as a standard-build plugin. Do not add the local
debug logger to `src/main`; CSV run logging belongs only in `src/test`.

## Plugin repository

1. Confirm `https://github.com/GalesongAS/tithe-farm-coach` is public.
2. Keep `.gradle`, `build`, IDE files, local logs, and JAR files untracked.
3. Keep the public author in `runelite-plugin.properties` set to `GalesongAS`.
4. Keep `build=standard`; the plugin has no third-party runtime dependencies.
5. Run `gradlew.bat clean build` on Windows or `./gradlew clean build` elsewhere.
6. Run the development client with `gradlew.bat run` and manually test every
   method. A successful build does not prove in-game behavior.
7. Commit and push the tested version.

## Review scope

- Java 11 source only
- No mouse, keyboard, menu, or movement automation
- No reflection, JNI, subprocesses, runtime downloads, or external services
- No gameplay simulation or injected clicks
- No production file logging or account-data transmission
- Uses fixed in-game object/item IDs rather than user-provided IDs
- Active only in the Tithe Farm region

The plugin observes RuneLite game state and draws overlays. Every gameplay
action still requires a normal player click.

## Plugin Hub pull request

1. Fork [runelite/plugin-hub](https://github.com/runelite/plugin-hub).
2. Create `plugins/tithe-farm-coach` in the fork with:

   ```text
   repository=https://github.com/GalesongAS/tithe-farm-coach.git
   commit=FULL_40_CHARACTER_COMMIT_HASH
   ```

3. Open one pull request to `runelite/plugin-hub` and keep subsequent fixes in
   that same pull request.
4. Address both the build workflow and RuneLite Plugin Hub Checks results.

## Suggested pull-request summary

> Tithe Farm Coach is a read-only, one-step-at-a-time Tithe Farm route guide.
> Existing plugins provide excellent timers and tracking; this plugin focuses
> on a Quest Helper-style flow that highlights one plant, water, harvest,
> deposit, or refill action and advances as game state changes. It supports five
> routes with progressively tighter timing. It does not inject input, alter
> menus, communicate with external services, or write production gameplay logs.

The Plugin Hub maintainers make the final policy and quality determination.
