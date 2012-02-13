<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- XSL to display login options as defined in realms.xml -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation">

  <xsl:include href="MyCoReLayout.xsl" />

  <xsl:variable name="PageTitle" select="i18n:translate('component.user2.login.change')" />
  <xsl:variable name="PageID" select="'login'" />

  <xsl:variable name="breadcrumb.extensions">
    <item label="{i18n:translate('component.user2.login.change')}" />
  </xsl:variable>

  <xsl:template match="realms">
    <div class="section">
      <p>
        <xsl:value-of select="i18n:translate('component.user2.login.currentAccount')" />
        <xsl:text> </xsl:text>
        <b>
          <xsl:choose>
            <xsl:when test="@guest='true'">
              <xsl:value-of select="i18n:translate('component.user2.login.guest')" />
              .
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="concat(@user,' [',@realm,']')" />
              .
            </xsl:otherwise>
          </xsl:choose>
        </b>
      </p>
    </div>
    <div class="section" id="sectionlast">
      <p>
        <b>
          <xsl:value-of select="i18n:translate('component.user2.login.select')" />
        </b>
        <br />
        <ul class="realms">
          <xsl:if test="@guest != 'true'">
            <li>
              <a href="{$ServletsBaseURL}logout">
                <xsl:value-of select="i18n:translate('component.user2.login.logout')" />
                <xsl:text> </xsl:text>
                <xsl:value-of select="i18n:translate('component.user2.login.guestUser')" />
              </a>
              <div>
                <xsl:value-of select="i18n:translate('component.user2.login.openAccess')" />
              </div>
            </li>
          </xsl:if>
          <xsl:for-each select="realm">
            <li>
              <xsl:for-each select="login">
                <xsl:variable name="lang">
                  <xsl:call-template name="selectPresentLang">
                    <xsl:with-param name="nodes" select="label" />
                  </xsl:call-template>
                </xsl:variable>
                <a href="{@url}">
                  <xsl:value-of select="label[lang($lang)]" />
                </a>
              </xsl:for-each>
              <xsl:for-each select="info">
                <xsl:variable name="lang">
                  <xsl:call-template name="selectPresentLang">
                    <xsl:with-param name="nodes" select="label" />
                  </xsl:call-template>
                </xsl:variable>
                <div>
                  <xsl:value-of select="label[lang($lang)]" />
                </div>
              </xsl:for-each>
            </li>
          </xsl:for-each>
          <xsl:if test="not(@createAccount = 'false')">
            <li>
              <a href="{$WebApplicationBaseURL}authoring/createAccount.xml?step=createAccount">
                <xsl:value-of select="i18n:translate('component.user2.login.useraccount')" />
              </a>
              <div>
                <xsl:value-of select="i18n:translate('component.user2.login.useraccountText')" />
              </div>
            </li>
          </xsl:if>
        </ul>
      </p>
      <p>
        <form method="get" action="{$ServletsBaseURL}MCRLoginServlet" class="action">
          <input value="cancel" name="action" type="hidden" />
          <input value="{i18n:translate('button.cancel')}" class="action" type="submit" />
        </form>
      </p>
    </div>
  </xsl:template>

</xsl:stylesheet>
