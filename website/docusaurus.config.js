// @ts-check
const { themes } = require('prism-react-renderer');

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'Harpia Arch',
  tagline: 'Arquitetura Java para workflows tipados sobre Quarkus.',
  favicon: 'img/logo.png',

  url: 'https://krisner94.github.io',
  baseUrl: '/Harpia-Arch/',
  organizationName: 'Krisner94',
  projectName: 'Harpia-Arch',
  trailingSlash: true,

  onBrokenLinks: 'throw',
  markdown: {
    mermaid: true,
    hooks: {
      onBrokenMarkdownLinks: 'warn',
    },
  },

  i18n: {
    defaultLocale: 'pt',
    locales: ['pt', 'en'],
    localeConfigs: {
      pt: {
        label: 'Português',
        direction: 'ltr',
        htmlLang: 'pt-BR',
      },
      en: {
        label: 'English',
        direction: 'ltr',
        htmlLang: 'en',
      },
    },
  },

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          routeBasePath: '/',
          sidebarPath: require.resolve('./sidebars.js'),
          editUrl: 'https://github.com/Krisner94/Harpia-Arch/edit/main/website/',
          showLastUpdateTime: false,
        },
        blog: false,
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      }),
    ],
  ],

  themes: [
    [
      '@easyops-cn/docusaurus-search-local',
      {
        hashed: true,
        indexBlog: false,
        language: ['pt', 'en'],
        docsRouteBasePath: '/',
        highlightSearchTermsOnTargetPage: true,
        explicitSearchResultPath: true,
      },
    ],
    '@docusaurus/theme-mermaid',
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      image: 'img/logo.png',
      metadata: [
        {
          name: 'keywords',
          content: 'Java, Quarkus, workflow, activities, architecture, Harpia Arch',
        },
      ],
      colorMode: {
        defaultMode: 'light',
        disableSwitch: false,
        respectPrefersColorScheme: true,
      },
      navbar: {
        title: 'Harpia Arch',
        hideOnScroll: true,
        logo: {
          alt: 'Harpia Arch',
          src: 'img/logo.png',
        },
        items: [
          {
            type: 'docSidebar',
            sidebarId: 'docs',
            position: 'left',
            label: 'Documentação',
          },
          {
            href: 'https://javadoc.io/doc/io.github.krisner94/harpia-core/1.0.0/',
            label: 'Javadoc',
            position: 'right',
          },
          {
            href: 'https://github.com/Krisner94/Harpia-Arch',
            label: 'GitHub',
            position: 'right',
          },
          {
            type: 'localeDropdown',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        links: [],
        copyright: `Copyright © ${new Date().getFullYear()} Rhama Krisner.`,
      },
      prism: {
        theme: themes.github,
        darkTheme: themes.dracula,
        additionalLanguages: ['java', 'markup', 'bash', 'json', 'yaml', 'properties'],
      },
      tableOfContents: {
        minHeadingLevel: 2,
        maxHeadingLevel: 3,
      },
      mermaid: {
        theme: {
          light: 'neutral',
          dark: 'dark',
        },
      },
    }),
};

module.exports = config;
