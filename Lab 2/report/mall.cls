\NeedsTeXFormat{LaTeX2e}
\ProvidesClass{mall}[2017/02/04 TDTS04 Report template]

\DeclareOption*{\PassOptionsToClass{\CurrentOption}{article}}
\ProcessOptions\relax

\LoadClass{article}

\RequirePackage{fontspec}
\RequirePackage[usenames,x11names,svgnames]{xcolor}
\RequirePackage{fancyhdr}
\RequirePackage[colorlinks=false, hidelinks]{hyperref}
\RequirePackage{tabularx}
\RequirePackage{titling}
\RequirePackage{fullpage}
\RequirePackage{lastpage}
\RequirePackage{tikz}
\RequirePackage[swedish]{babel}
\setlength{\headsep}{20pt}
\setlength{\headheight}{40pt}

\pagestyle{fancy}
\newlength{\borderwidth}
\setlength{\borderwidth}{3cm}

\renewcommand{\maketitlehooka}{%
  \begin{tikzpicture}[overlay]
    \node [anchor=south west] (lith) at (-3,1) {\includegraphics[width=\borderwidth]{lith}};
    \node [rectangle,fill=black!5!white,minimum height=21cm,text width=\borderwidth,anchor=north west] (border) at (lith.south west) {};
    \node [anchor=south west] at (border.south west) {\includegraphics[width=\borderwidth]{ida}};
   \end{tikzpicture}
   }
\pretitle{\begin{center}{\color{DarkBlue}\usefont{T1}{arial}{bx}{n}\fontsize{28pt}{1em}\selectfont TDTS04 Computer Networks and Distributed Systems}\end{center}\vspace{100pt}\begin{center}\Huge\bfseries}
\posttitle{\end{center}}
\preauthor{\begin{center}\large Author\end{center}\begin{center}\normalsize}
\postauthor{\end{center}\vfill}

\postdate{\end{center}}
\lhead{\thedate}
\chead{\thetitle}
\lfoot{\version{}}
\rfoot{\thepage\ / \pageref{LastPage}}
\cfoot{}

\newcommand{\projectpage}{%
  \begin{titlingpage}%
    \addtolength{\textwidth}{-3cm}\addtolength{\hoffset}{2cm}%
    \maketitle%
  \end{titlingpage}%
}

\endinput
