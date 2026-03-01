# AVERTROX-SHOPs

AvertoxShop is a Minecraft auction and admin shop plugin with a branded interactive GUI.

## Updates (Commit 2 -> Commit 15)

### Commit 2
- Added full AvertoxShop project source code to GitHub.
- Added `.gitignore` for build/IDE files.

### Commit 3
- Cleaned and updated `README.md` structure/content.
- Removed outdated build section.

### Commit 4
- Fixed private chat handling for shop input (price/name/quantity).
- Improved quantity controls in the sell/create process.

### Commit 5
- Added a dedicated sell-input inventory where players can drag one item type and continue listing flow.
- Improved buy menu structure to better match creative-category navigation.
- Added refresh controls and improved live reload behavior for auction browsing.
- Strengthened pagination/live listing navigation reliability when new items are listed.

### Commit 6
- Implemented creative-tab based category selector with category-specific item browsing.
- Added anvil-based input for setting listing price and name (replacing chat for these actions).

### Commit 7
- Improved stability and polishing for creative category menu and anvil input flow.
- Finalized push of these UI/input workflow updates.

### Commit 8
- Fixed anvil input reliability by reading rename text directly from the anvil field.
- Added anvil input support for quantity changes in sell flow.
- Polished creative category menu visuals (better labels, decorative style, clearer category cards).
- Process update: README updates section will be updated on every commit.

### Commit 9
- Fixed anvil input compatibility issue where some servers showed "Anvil input unavailable".
- Reworked anvil text extraction to use inventory slot fallback for broader Spigot/Paper compatibility.

### Commit 10
- Improved anvil submit reliability:
  - supports result-slot detection by slot type
  - auto-submits on anvil close when text is present
  - reflection fallback for `getRenameText` when available
- Improved parser tolerance:
  - prices now accept `$100`, `100.00`, and comma formats
  - quantities accept comma/space formatted input

### Commit 11
- Added `PrepareAnvilEvent` handling for stable typed-text capture across server variants.
- Ensured anvil result is always generated for submission and cached per-player.
- Fixed submit/close behavior so name, quantity, and price changes apply consistently.

### Commit 12
- Removed strict anvil holder dependence for submit/capture handling.
- Routed anvil processing by inventory type + pending shop input state for cross-server reliability.
- Hardened price parsing to accept symbols/formatting by stripping non-numeric characters safely.

### Commit 13
- Fixed anvil close handling to work even when custom inventory holders are not preserved by the server.
- Added direct ANVIL-type close routing with pending-input checks to ensure value submission is not dropped.

### Commit 14
- Reverted sell/edit input from anvil back to chat prompts (price, name, quantity) from the menu.
- Kept chat handling private so only the inputting player sees their messages and responses.

### Commit 15 (Current)
- Reworked creative category assignment with explicit material-pattern mapping to prevent everything falling into Building Blocks.
- Added buy-quantity selection menu when purchasing a listing: `1`, `32`, `64`, or `All`.
- Added proportional purchase pricing and partial listing fulfillment (remaining stock stays listed).

## Features
- Players can sell items in the auction.
- Players can buy auction listings.
- Players can manage their own listings:
  - change price
  - change listing name
  - add more items to stackable listings
- Enchanted/custom-meta items are preserved (full `ItemMeta` is kept).
- Buy menu is structured by creative inventory categories.
- Pagination is implemented for large listing counts (more than one double chest).

## Admin Shop
- Players can buy from admin shop listings.
- Only admins can create admin shop listings.
- Admin check: OP or permission `avertox.shop.admin`.

## Command
- `/avertoxshop`
- Aliases: `/avshop`, `/auction`

## Architecture (Easy API Swap Later)
The plugin uses interfaces so you can replace internals with your own systems:

- `MoneyService`
  - `has(UUID, amount)`
  - `withdraw(UUID, amount, reason)`
  - `deposit(UUID, amount, reason)`
  - `getBalance(UUID)`

- `PermissionService`
  - `has(UUID, permission)`
  - `isAdmin(UUID)`

Default implementations included:
- `SimpleMoneyService` (in-memory, development only)
- `BukkitPermissionService`

## Main Files
- `src/main/java/dev/avertox/shop/AvertoxShopPlugin.java`
- `src/main/java/dev/avertox/shop/service/ShopService.java`
- `src/main/java/dev/avertox/shop/gui/MenuController.java`
- `src/main/resources/plugin.yml`

## License
This project is proprietary. See the `LICENSE` file for full terms.

