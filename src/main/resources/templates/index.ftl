<h2>Videos</h2>
<ul>
<#list videos as video>
    <li>
        <a href="/${video.location}">${video.fileName}</a>
    </li>
</#list>
</ul>