# AVERTROX-SHOPs

AvertoxShop is a Minecraft auction and admin shop plugin with a branded interactive GUI.

## Updates (Commit 2 -> Commit 5)

### Commit 2
- Added full AvertoxShop project source code to GitHub.
- Added `.gitignore` for build/IDE files.

### Commit 3
- Cleaned and updated `README.md` structure/content.
- Removed outdated build section.

### Commit 4
- Fixed private chat handling for shop input (price/name/quantity).
- Improved quantity controls in the sell/create process.

### Commit 5 (Current)
- Added a dedicated sell-input inventory where players can drag one item type and continue listing flow.
- Improved buy menu structure to better match creative-category navigation.
- Added refresh controls and improved live reload behavior for auction browsing.
- Strengthened pagination/live listing navigation reliability when new items are listed.

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

