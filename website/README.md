# Harpia Arch documentation

Documentation site built with Docusaurus 3.

## Development

```bash
cd website
npm install
npm start
```

The server runs at `http://localhost:3000/Harpia-Arch/`.

## Production build

```bash
cd website
npm run build
npm run serve
```

The static output is generated in `website/build`.

## Content

- Portuguese is the default locale and lives in `website/docs`.
- English translations live in `website/i18n/en/docusaurus-plugin-content-docs/current`.
- Navigation is declared in `website/sidebars.js`.
- GitHub Pages deployment is handled by `.github/workflows/docs.yml`.
