# Tithe Farm Coach

Tithe Farm Coach is a read-only, one-step-at-a-time route guide for Tithe Farm.
It is intended to make the minigame feel closer to using Quest Helper: instead
of remembering an entire route, the player follows a single highlighted action,
then the coach advances to the next one.

There are already excellent Tithe Farm timer and tracking plugins. This plugin
goes a step further by combining plant state, inventory state, route order, and
timing into a continuously updated **plant here, water here, harvest here**
instruction. It does not perform any action for the player.

## Features

- Five routes ranging from forgiving beginner practice to time-tight advanced play
- Action labels for planting, watering, harvesting, depositing, and refilling
- Color-coded plots and readable water/death timers
- Water-dose checks, including Gricoller's can charges
- Fruit, reward-point, and permanent-shop progress

## Supported methods

| Method | Difficulty | What changes |
| --- | --- | --- |
| Relaxed 8 | Most forgiving | Small practice route with ample recovery time |
| Safe 16 | Forgiving | Longer route while retaining generous timing leeway |
| Standard 20x5 | Moderate | Five 20-plant batches for a conventional 100-fruit set |
| Simple 23 | Tight | More plants per circuit and less time to recover from delays |
| Advanced 25x4 | Tightest | Four 25-plant batches with the least timing leeway |

Difficulty refers to timing pressure, not additional mechanics. Every method
uses the same plant, water, maintain, harvest, deposit, and refill actions.

The coach does not click, move the mouse, alter menu entries, select inventory
items, or send keyboard input. Every game action remains with the player.

## Plot colors

- Blue: empty
- Green: watered
- Orange: needs water
- Red: close to dying
- Purple: ready to harvest
- Dark red: dead

## Privacy

The plugin does not write gameplay logs, make network requests, or transmit
account or gameplay data.

## License

BSD 2-Clause License. See [LICENSE](LICENSE).
