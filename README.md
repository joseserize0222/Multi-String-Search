# <img src="src/main/resources/META-INF/pluginIcon.svg" alt="Icon" align="right" style="height: 43px;"> Multi-String Search Plugin

The Multi-String Search Plugin enables fast and efficient searching of multiple strings within files directly in the IntelliJ IDEA environment. This plugin provides an intuitive user interface to input multiple search patterns, navigate through matches, and track file changes, making it an excellent tool for code analysis and pattern detection.

## Features

- **Multi-String Search**: Quickly search for multiple strings within files, displaying all occurrences with precise line numbers.
- **Navigable Search Results**: Click on any result to jump directly to the corresponding line in the file.

![Demo](./src/data/Multi-String%20Search%20and%20Navigation.gif)

- **Efficient Search Algorithm**: Uses the Aho-Corasick algorithm to handle multi-pattern matching efficiently, even for large files.
- **Debounced Real-Time Updates**: Provides real-time updates when files are edited or new files are selected, ensuring search results are always relevant.

![Demo](./src/data/Live%20Updates%20on%20File%20Change.gif)

## Main Components

### `MyToolWindow.kt`

- **Purpose**: The primary UI container for the plugin, displaying input fields for patterns and search results.
- **Implementation Details**:
  - Listens for file selection changes and automatically updates results based on current patterns.
  - Adds document listeners to update search results when the current file is modified.

### `SearchManager.kt`

- **Purpose**: Core logic for executing multi-string searches and managing performance optimizations.
- **Implementation Details**:
  - Coordinates with PatternInputPanel to retrieve search patterns and ResultsPanel to display results, ensuring seamless interaction between components.
    - Utilizes the Aho-Corasick algorithm for efficient searching and implements a debounce mechanism for responsive updates.

### `PatternInputPanel.kt`

- **Purpose**: UI component for entering multiple search patterns.
- **Implementation Details**:
  - Provides a simple input area where users can type patterns, and includes a method that the SearchManager uses to retrieve these patterns for processing.

### `ResultsPanel.kt`

- **Purpose**: Displays the search results in an organized list within the IntelliJ IDEA tool window, allowing users to navigate directly to each result in the code.
- **Implementation Details**:
  - Includes a method that the SearchManager uses to update the UI once the search results are ready.

## General Architecture

### 1. **Aho-Corasick Algorithm**:
The plugin leverages the Aho-Corasick algorithm to perform fast multi-pattern searches, handling large files and multiple patterns with high efficiency.

### 2. **Debounced Real-Time Updates**:
By using a debounce delay, the plugin avoids excessive computations while providing real-time search results as files are edited. This debounce mechanism is managed within SearchManager.

### 3. **User Interface**:
The UI includes PatternInputPanel for search input and ResultsPanel for displaying results, both organized within MyToolWindow.

## Benefits for Developers

- **Enhanced Code Analysis**: Quickly identify multiple patterns within files, saving time during code reviews or when working on large codebases.
- **Real-Time Feedback**: Get immediate search results even as files are being modified, offering a smooth and uninterrupted workflow.
- **Streamlined Navigation**: Navigate directly to each match, making it easier to explore instances of patterns across the code.

## Recent Changes

- **Enhanced UI Feedback**: Improved real-time feedback and file-change detection for a more responsive experience.
- **Improved Debounce Mechanism**: Added a delay to prevent excessive updates, enhancing performance with rapid file changes.

## Installation

*Note: The options for installation using the IDE Plugin System and from JetBrains Marketplace are not yet available.*

- **Using the IDE built-in plugin system**:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Multi-String Search"</kbd> > <kbd>Install</kbd>

- **Using JetBrains Marketplace**:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- **Manually**:

  Download the [latest release](https://github.com/Alain-David-001/Multi-String-Search/releases/latest) and install it manually using  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Acknowledgements

This plugin was created using the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template). Special thanks to JetBrains for providing comprehensive tools and templates for plugin development.
