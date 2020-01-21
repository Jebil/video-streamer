<h2>Videos</h2>
<ul>

<#list videos as video>
    <li>
        <a href="/${video.path}">${video.title}</a>
    </li>
</#list>
</ul>